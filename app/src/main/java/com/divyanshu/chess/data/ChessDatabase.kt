package com.divyanshu.chess.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GameEntity::class], version = 3, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        /** Adds the per-player clocks, giving saved games a fresh ten minutes each. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN whiteTimeMs INTEGER NOT NULL DEFAULT 600000")
                db.execSQL("ALTER TABLE games ADD COLUMN blackTimeMs INTEGER NOT NULL DEFAULT 600000")
            }
        }

        /** Remembers which engine each saved game was played against; older games used the built-in one. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN engine INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
