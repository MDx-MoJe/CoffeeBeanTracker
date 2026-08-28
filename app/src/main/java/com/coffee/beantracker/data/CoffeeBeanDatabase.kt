package com.coffee.beantracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CoffeeBean::class, DeductRecord::class, GreenBean::class, com.coffee.beantracker.bridge.RoastConsumeEntity::class], version = 10, exportSchema = false)
abstract class CoffeeBeanDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao

    abstract fun deductRecordDao(): DeductRecordDao

    abstract fun greenBeanDao(): GreenBeanDao

    abstract fun roastConsumeDao(): com.coffee.beantracker.bridge.RoastConsumeDao

    companion object {
        @Volatile
        private var INSTANCE: CoffeeBeanDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS coffee_beans_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "roastDate INTEGER NOT NULL, " +
                            "restDays INTEGER NOT NULL, " +
                            "bestBeforeDays INTEGER NOT NULL, " +
                            "processMethod TEXT NOT NULL DEFAULT '', " +
                            "roastLevel TEXT NOT NULL DEFAULT '', " +
                            "origin TEXT NOT NULL DEFAULT '', " +
                            "flavorNotes TEXT NOT NULL DEFAULT '', " +
                            "developmentTime TEXT NOT NULL DEFAULT '', " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL(
                    "INSERT INTO coffee_beans_new (id, name, roastDate, restDays, bestBeforeDays, processMethod, roastLevel, origin, flavorNotes, createdAt) " +
                            "SELECT id, name, roastDate, restDays, bestBeforeDays, variety, roastLevel, origin, flavorNotes, createdAt FROM coffee_beans"
                )
                database.execSQL("DROP TABLE coffee_beans")
                database.execSQL("ALTER TABLE coffee_beans_new RENAME TO coffee_beans")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN imagePath TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN backgroundImagePath TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN stockGrams INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN deductGrams INTEGER NOT NULL DEFAULT 18")
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS deduct_records (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "beanId INTEGER NOT NULL DEFAULT 0, " +
                            "beanName TEXT NOT NULL DEFAULT '\\'\\'', " +
                            "gramsDeducted INTEGER NOT NULL DEFAULT 0, " +
                            "stockBefore INTEGER NOT NULL DEFAULT 0, " +
                            "stockAfter INTEGER NOT NULL DEFAULT 0, " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }


        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN pourOverGrams INTEGER NOT NULL DEFAULT 15")
                database.execSQL("ALTER TABLE coffee_beans ADD COLUMN espressoGrams INTEGER NOT NULL DEFAULT 18")
                database.execSQL("ALTER TABLE deduct_records ADD COLUMN brewType TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS green_beans (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "origin TEXT NOT NULL DEFAULT '', " +
                            "processMethod TEXT NOT NULL DEFAULT '', " +
                            "variety TEXT NOT NULL DEFAULT '', " +
                            "purchaseDate INTEGER NOT NULL DEFAULT 0, " +
                            "purchaseGrams INTEGER NOT NULL DEFAULT 0, " +
                            "remainingGrams INTEGER NOT NULL DEFAULT 0, " +
                            "notes TEXT NOT NULL DEFAULT '', " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE green_beans ADD COLUMN altitude TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE green_beans ADD COLUMN grade TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE green_beans ADD COLUMN harvestYear TEXT NOT NULL DEFAULT ''")
            }
        }

        // 克数字段 Int→Double：三张表重建（SQLite ALTER 无法改列类型）
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS coffee_beans_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "roastDate INTEGER NOT NULL, " +
                            "restDays INTEGER NOT NULL, " +
                            "bestBeforeDays INTEGER NOT NULL, " +
                            "processMethod TEXT NOT NULL DEFAULT '', " +
                            "roastLevel TEXT NOT NULL, " +
                            "origin TEXT NOT NULL DEFAULT '', " +
                            "flavorNotes TEXT NOT NULL DEFAULT '', " +
                            "developmentTime TEXT NOT NULL DEFAULT '', " +
                            "stockGrams REAL NOT NULL DEFAULT 0.0, " +
                            "deductGrams REAL NOT NULL DEFAULT 18.0, " +
                            "pourOverGrams REAL NOT NULL DEFAULT 15.0, " +
                            "espressoGrams REAL NOT NULL DEFAULT 18.0, " +
                            "imagePath TEXT NOT NULL DEFAULT '', " +
                            "backgroundImagePath TEXT NOT NULL DEFAULT '', " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL(
                    "INSERT INTO coffee_beans_new (id, name, roastDate, restDays, bestBeforeDays, processMethod, roastLevel, origin, flavorNotes, developmentTime, stockGrams, deductGrams, pourOverGrams, espressoGrams, imagePath, backgroundImagePath, createdAt) " +
                            "SELECT id, name, roastDate, restDays, bestBeforeDays, processMethod, roastLevel, origin, flavorNotes, developmentTime, stockGrams, deductGrams, pourOverGrams, espressoGrams, imagePath, backgroundImagePath, createdAt FROM coffee_beans"
                )
                database.execSQL("DROP TABLE coffee_beans")
                database.execSQL("ALTER TABLE coffee_beans_new RENAME TO coffee_beans")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS deduct_records_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "beanId INTEGER NOT NULL DEFAULT 0, " +
                            "beanName TEXT NOT NULL DEFAULT '', " +
                            "gramsDeducted REAL NOT NULL DEFAULT 0.0, " +
                            "stockBefore REAL NOT NULL DEFAULT 0.0, " +
                            "stockAfter REAL NOT NULL DEFAULT 0.0, " +
                            "brewType TEXT NOT NULL DEFAULT '', " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL(
                    "INSERT INTO deduct_records_new (id, beanId, beanName, gramsDeducted, stockBefore, stockAfter, brewType, createdAt) " +
                            "SELECT id, beanId, beanName, gramsDeducted, stockBefore, stockAfter, brewType, createdAt FROM deduct_records"
                )
                database.execSQL("DROP TABLE deduct_records")
                database.execSQL("ALTER TABLE deduct_records_new RENAME TO deduct_records")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS green_beans_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "origin TEXT NOT NULL DEFAULT '', " +
                            "processMethod TEXT NOT NULL DEFAULT '', " +
                            "variety TEXT NOT NULL DEFAULT '', " +
                            "altitude TEXT NOT NULL DEFAULT '', " +
                            "grade TEXT NOT NULL DEFAULT '', " +
                            "harvestYear TEXT NOT NULL DEFAULT '', " +
                            "purchaseDate INTEGER NOT NULL DEFAULT 0, " +
                            "purchaseGrams REAL NOT NULL DEFAULT 0.0, " +
                            "remainingGrams REAL NOT NULL DEFAULT 0.0, " +
                            "notes TEXT NOT NULL DEFAULT '', " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL(
                    "INSERT INTO green_beans_new (id, name, origin, processMethod, variety, altitude, grade, harvestYear, purchaseDate, purchaseGrams, remainingGrams, notes, createdAt) " +
                            "SELECT id, name, origin, processMethod, variety, altitude, grade, harvestYear, purchaseDate, purchaseGrams, remainingGrams, notes, createdAt FROM green_beans"
                )
                database.execSQL("DROP TABLE green_beans")
                database.execSQL("ALTER TABLE green_beans_new RENAME TO green_beans")
            }
        }

        // v10：互联桥接幂等表（烤豆→豆袋烘焙消耗扣减）
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS roast_consumes (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "roastId TEXT NOT NULL, " +
                            "greenBeanId INTEGER NOT NULL DEFAULT 0, " +
                            "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_roast_consumes_roastId ON roast_consumes (roastId)"
                )
            }
        }

        fun getDatabase(context: Context): CoffeeBeanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CoffeeBeanDatabase::class.java,
                    "coffee_bean_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}