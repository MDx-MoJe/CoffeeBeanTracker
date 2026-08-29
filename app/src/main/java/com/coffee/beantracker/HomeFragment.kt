package com.coffee.beantracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import android.content.res.Configuration
import androidx.recyclerview.widget.GridLayoutManager
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.GreenBean
import com.coffee.beantracker.databinding.FragmentHomeBinding
import com.coffee.beantracker.utils.BluetoothPrinterManager
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.utils.PrintFlowHelper
import com.coffee.beantracker.utils.ToastCustom
import com.coffee.beantracker.viewmodel.CoffeeBeanViewModel
import com.coffee.beantracker.viewmodel.GreenBeanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CoffeeBeanAdapter
    private lateinit var db: CoffeeBeanDatabase
    private lateinit var viewModel: CoffeeBeanViewModel
    private lateinit var greenBeanAdapter: GreenBeanAdapter
    private lateinit var greenBeanViewModel: GreenBeanViewModel
    private var isGreenTab = false
    /** Tab 切换回调（通知 MainActivity 以决定「添加」按钮跳转目标） */
    var onTabChanged: ((Boolean) -> Unit)? = null

    private lateinit var printerMgr: BluetoothPrinterManager
    private lateinit var printFlow: PrintFlowHelper

    // Bluetooth permissions & enable launchers — must be initialized before onStart (class property init)
    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.all { it.value }
        if (allGranted) {
            printFlow.onPermissionsGranted()
        } else {
            val ctx = context ?: return@registerForActivityResult
            ToastCustom.show(ctx, ctx.getString(R.string.bluetooth_perm_denied), android.widget.Toast.LENGTH_LONG)
        }
    }

    private val btEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            printFlow.onBluetoothEnabled()
        } else {
            val ctx = context ?: return@registerForActivityResult
            ToastCustom.show(ctx, ctx.getString(R.string.bluetooth_off))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        db = CoffeeBeanDatabase.getDatabase(ctx)

        printerMgr = BluetoothPrinterManager(ctx.applicationContext)
        printFlow = PrintFlowHelper.from(this, printerMgr, btPermissionLauncher, btEnableLauncher)

        val factory = object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return CoffeeBeanViewModel(requireActivity().application) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[CoffeeBeanViewModel::class.java]

        setupRecyclerView()
        setupViewModel()
        setupGreenRecyclerView()
        setupGreenViewModel()
        setupTabBar()
        applyTabSelection(false)
    }

    override fun onDestroyView() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { printerMgr.disconnect() }
        }
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        val ctx = requireContext()
        val span = getSpanCountForCurrentWidth()
        val lm = GridLayoutManager(ctx, span)
        binding.recyclerView.layoutManager = lm

        view?.addOnLayoutChangeListener { _, _, _, _, _, oldLeft, oldTop, oldRight, oldBottom ->
            val oldW = oldRight - oldLeft
            val newW = binding.root.width
            if (oldW > 0 && oldW != newW) {
                val newSpan = getSpanCountForCurrentWidth()
                val curLm = binding.recyclerView.layoutManager as? GridLayoutManager
                if (curLm != null && curLm.spanCount != newSpan) {
                    curLm.spanCount = newSpan
                }
            }
        }
        adapter = CoffeeBeanAdapter(
            onItemClick = { bean ->
                val intent = Intent(ctx, AddEditBeanActivity::class.java).apply {
                    putExtra(AddEditBeanActivity.EXTRA_BEAN_ID, bean.id)
                }
                startActivity(intent)
            },
            onDeductPourOver = { bean -> deduct(bean, "POUR_OVER") },
            onDeductEspresso = { bean -> deduct(bean, "ESPRESSO") },
            onModifyStock = { bean, grams -> modifyStock(bean, grams) },
            onPrintClick = { bean -> printFlow.requestBluetoothPermissionsThenPrint(bean, copies = 1) }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun modifyStock(bean: CoffeeBean, grams: Double) {
        val old = bean.stockGrams
        val idRef = bean.id
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.coffeeBeanDao().updateStockGrams(idRef, grams)
            }
            val ctx = context ?: return@launch
            val delta = grams - old
            val sign = if (delta >= 0) "+" else ""
            ToastCustom.show(ctx, "库存更新：${GramFormatter.format(old)} → ${GramFormatter.format(grams)}（$sign${GramFormatter.format(delta)}）")
        }
    }

    private fun deduct(bean: CoffeeBean, brewType: String) {
        val grams = when (brewType) {
            "POUR_OVER" -> if (bean.pourOverGrams > 0) bean.pourOverGrams else 15.0
            else -> if (bean.espressoGrams > 0) bean.espressoGrams else 18.0
        }
        val newStock = bean.stockGrams - grams
        val safeStock: Double = if (newStock < 0) 0.0 else newStock
        val stockBefore: Double = bean.stockGrams
        val beanName = bean.name
        val beanIdRef = bean.id
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.coffeeBeanDao().updateStockGrams(beanIdRef, safeStock)
                val record = com.coffee.beantracker.data.DeductRecord(
                    beanId = beanIdRef,
                    beanName = beanName,
                    gramsDeducted = grams,
                    stockBefore = stockBefore,
                    stockAfter = safeStock,
                    brewType = brewType
                )
                db.deductRecordDao().insert(record)
                val count = db.deductRecordDao().getCount()
                if (count > 99) db.deductRecordDao().deleteOldest(count - 99)
            }
            val ctx = context ?: return@launch
            ToastCustom.show(ctx, ctx.getString(R.string.stock_deduct_banner, GramFormatter.formatWithUnit(grams)))
        }
    }

    private fun setupViewModel() {
        binding.emptyTextView.visibility = View.GONE   // 先归零，等 LiveData 首回调统一决定，避免首帧闪烁重影
        viewModel.allBeans.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyTextView.visibility = if (!isGreenTab && list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupGreenRecyclerView() {
        val ctx = requireContext()
        val span = getSpanCountForCurrentWidth()
        binding.greenBeanRecyclerView.layoutManager = GridLayoutManager(ctx, span)
        greenBeanAdapter = GreenBeanAdapter(
            onItemClick = { bean ->
                val intent = Intent(ctx, AddEditGreenBeanActivity::class.java).apply {
                    putExtra(AddEditGreenBeanActivity.EXTRA_BEAN_ID, bean.id)
                }
                startActivity(intent)
            }
        )
        binding.greenBeanRecyclerView.adapter = greenBeanAdapter
    }

    private fun setupGreenViewModel() {
        val factory = object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return GreenBeanViewModel(requireActivity().application) as T
            }
        }
        greenBeanViewModel = ViewModelProvider(this, factory)[GreenBeanViewModel::class.java]
        binding.greenBeanEmptyView.visibility = View.GONE   // 同上：先归零防闪烁
        greenBeanViewModel.allGreenBeans.observe(viewLifecycleOwner) { list ->
            greenBeanAdapter.submitList(list)
            binding.greenBeanEmptyView.visibility = if (isGreenTab && list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupTabBar() {
        binding.tabGreen.setOnClickListener { applyTabSelection(true) }
        binding.tabRoasted.setOnClickListener { applyTabSelection(false) }
    }

    private fun applyTabSelection(green: Boolean) {
        isGreenTab = green
        val ctx = requireContext()
        val selectedColor = ContextCompat.getColor(ctx, R.color.dock_selected_fg)
        val secondaryColor = ContextCompat.getColor(ctx, R.color.text_secondary)
        binding.tabGreen.setTextColor(if (green) selectedColor else secondaryColor)
        binding.tabGreen.setBackgroundResource(if (green) R.drawable.dock_pill_selected_home else R.drawable.dock_pill_bg)
        binding.tabRoasted.setTextColor(if (!green) selectedColor else secondaryColor)
        binding.tabRoasted.setBackgroundResource(if (!green) R.drawable.dock_pill_selected_home else R.drawable.dock_pill_bg)
        binding.greenBeanRecyclerView.visibility = if (green) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (green) View.GONE else View.VISIBLE
        binding.greenBeanEmptyView.visibility = if (green && greenBeanAdapter.currentList.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyTextView.visibility = if (!green && adapter.currentList.isEmpty()) View.VISIBLE else View.GONE
        onTabChanged?.invoke(green)
    }

    fun scrollToTop() {
        _binding?.recyclerView?.smoothScrollToPosition(0)
    }

    private fun getSpanCountForCurrentWidth(): Int {
        return try {
            val widthDp = resources.configuration.screenWidthDp
            if (widthDp >= 585) 2 else 1
        } catch (_: Throwable) {
            1
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view ?: return
        val newSpan = try {
            if (newConfig.screenWidthDp >= 585) 2 else 1
        } catch (_: Throwable) {
            1
        }
        val curLm = _binding?.recyclerView?.layoutManager as? GridLayoutManager
        if (curLm != null && curLm.spanCount != newSpan) {
            curLm.spanCount = newSpan
        }
    }
}
