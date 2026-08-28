package com.coffee.beantracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.coffee.beantracker.databinding.ActivityMainBinding
import com.coffee.beantracker.theme.ThemeManager
import com.coffee.beantracker.utils.ConsentManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var homeFragment: HomeFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var currentTab = TAB_HOME
    private var isGreenTab = false
    private var mainUiInited = false
    private var lastSavedInstanceState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 隐私政策门闩：未同意则弹窗拦截，同意后才初始化主界面（防重复初始化）
        if (!ConsentManager.ensureConsent(this) {
            initMainUi()
        }) return
        // 已同意：正常路径直接初始化
        lastSavedInstanceState = savedInstanceState
        initMainUi()
    }

    private fun initMainUi() {
        if (mainUiInited) return
        mainUiInited = true

        setSupportActionBar(binding.toolbar)
        applyToolbarColors()

        val restored = lastSavedInstanceState
        if (restored == null) {
            homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, homeFragment!!, TAG_HOME)
                .commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
            profileFragment = supportFragmentManager.findFragmentByTag(TAG_PROFILE) as? ProfileFragment
            currentTab = restored.getInt(STATE_CURRENT_TAB, TAB_HOME)
        }

        setupDock()
        updateDockSelection(currentTab)
        homeFragment?.onTabChanged = { green -> isGreenTab = green }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CURRENT_TAB, currentTab)
    }

    override fun onResume() {
        super.onResume()
        applyToolbarColors()
        updateDockSelection(currentTab)
    }

    private fun applyToolbarColors() {
        val colors = ThemeManager.getThemeColors(this)
        val translucentPrimary = (0xE6 shl 24) or (colors.colorPrimary and 0x00FFFFFF)
        binding.appbar.setBackgroundColor(translucentPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors, backgroundOverride = translucentPrimary)
        window.statusBarColor = colors.colorPrimaryDark
    }

    private fun setupDock() {
        binding.dockHome.setOnClickListener { switchTab(TAB_HOME) }
        binding.dockAdd.setOnClickListener {
            val intent = if (isGreenTab)
                Intent(this, AddEditGreenBeanActivity::class.java)
            else
                Intent(this, AddEditBeanActivity::class.java)
            startActivity(intent)
        }
        binding.dockProfile.setOnClickListener { switchTab(TAB_PROFILE) }
    }

    private fun switchTab(target: Int) {
        if (target == currentTab) {
            if (target == TAB_HOME) homeFragment?.scrollToTop()
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        when (currentTab) {
            TAB_HOME -> homeFragment?.let { transaction.hide(it) }
            TAB_PROFILE -> profileFragment?.let { transaction.hide(it) }
        }
        when (target) {
            TAB_HOME -> {
                if (homeFragment == null) {
                    homeFragment = HomeFragment()
                    transaction.add(R.id.fragmentContainer, homeFragment!!, TAG_HOME)
                } else {
                    transaction.show(homeFragment!!)
                }
                binding.toolbar.setTitle(R.string.app_name)
            }
            TAB_PROFILE -> {
                if (profileFragment == null) {
                    profileFragment = ProfileFragment()
                    transaction.add(R.id.fragmentContainer, profileFragment!!, TAG_PROFILE)
                } else {
                    transaction.show(profileFragment!!)
                }
                binding.toolbar.setTitle(R.string.settings)
            }
        }
        transaction.commitNow()
        currentTab = target
        updateDockSelection(target)
    }

    private fun updateDockSelection(which: Int) {
        val selectedColor = ContextCompat.getColor(this, R.color.dock_selected_fg)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        val homeActive = (which == TAB_HOME)
        if (homeActive) {
            binding.dockHomeContent.setBackgroundResource(R.drawable.dock_pill_selected_home)
            binding.ivDockHome.setColorFilter(selectedColor)
            binding.tvDockHome.setTextColor(selectedColor)
            binding.tvDockHome.paint.isFakeBoldText = true
        } else {
            binding.dockHomeContent.setBackgroundResource(R.drawable.dock_pill_bg)
            binding.ivDockHome.setColorFilter(secondaryColor)
            binding.tvDockHome.setTextColor(secondaryColor)
            binding.tvDockHome.paint.isFakeBoldText = false
        }

        val profileActive = (which == TAB_PROFILE)
        if (profileActive) {
            binding.dockProfileContent.setBackgroundResource(R.drawable.dock_pill_selected_profile)
            binding.ivDockProfile.setColorFilter(selectedColor)
            binding.tvDockProfile.setTextColor(selectedColor)
            binding.tvDockProfile.paint.isFakeBoldText = true
        } else {
            binding.dockProfileContent.setBackgroundResource(R.drawable.dock_pill_bg)
            binding.ivDockProfile.setColorFilter(secondaryColor)
            binding.tvDockProfile.setTextColor(secondaryColor)
            binding.tvDockProfile.paint.isFakeBoldText = false
        }
    }

    companion object {
        private const val TAG_HOME = "HOME"
        private const val TAG_PROFILE = "PROFILE"
        private const val STATE_CURRENT_TAB = "current_tab"
        private const val TAB_HOME = 0
        private const val TAB_PROFILE = 2
    }
}
