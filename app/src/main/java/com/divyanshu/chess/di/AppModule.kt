package com.divyanshu.chess.di

import android.content.Context
import androidx.room.Room
import com.divyanshu.chess.data.ChessDatabase
import com.divyanshu.chess.data.DataStoreSettingsRepository
import com.divyanshu.chess.data.GameDao
import com.divyanshu.chess.data.RealtimeTicker
import com.divyanshu.chess.data.RoomGameRepository
import com.divyanshu.chess.data.VibratorHaptics
import com.divyanshu.chess.domain.ai.AiProvider
import com.divyanshu.chess.domain.ai.EngineAvailability
import com.divyanshu.chess.domain.clock.ClockTicker
import com.divyanshu.chess.domain.feedback.Haptics
import com.divyanshu.chess.domain.repository.GameRepository
import com.divyanshu.chess.domain.repository.SettingsRepository
import com.divyanshu.chess.engine.DefaultAiProvider
import com.divyanshu.chess.engine.EngineDetails
import com.divyanshu.chess.engine.PositionAnalyzer
import com.divyanshu.chess.engine.SharedEngine
import com.divyanshu.chess.engine.StockfishAnalyzer
import com.divyanshu.chess.lab.AssetLabDataSource
import com.divyanshu.chess.lab.LabDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChessDatabase =
        Room.databaseBuilder(context, ChessDatabase::class.java, "chess.db")
            .addMigrations(ChessDatabase.MIGRATION_1_2, ChessDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideGameDao(database: ChessDatabase): GameDao = database.gameDao()

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindGameRepository(impl: RoomGameRepository): GameRepository

    @Binds
    abstract fun bindAiProvider(impl: DefaultAiProvider): AiProvider

    @Binds
    abstract fun bindEngineAvailability(impl: SharedEngine): EngineAvailability

    @Binds
    abstract fun bindEngineDetails(impl: SharedEngine): EngineDetails

    @Binds
    abstract fun bindPositionAnalyzer(impl: StockfishAnalyzer): PositionAnalyzer

    @Binds
    abstract fun bindLabDataSource(impl: AssetLabDataSource): LabDataSource

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindHaptics(impl: VibratorHaptics): Haptics

    @Binds
    abstract fun bindClockTicker(impl: RealtimeTicker): ClockTicker
}
