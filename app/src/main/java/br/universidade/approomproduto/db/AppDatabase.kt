package br.universidade.approomproduto.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.model.Product

// A8: version = 2 porque a entidade Product ganhou a coluna "quantity"
@Database(entities = [Product::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton: uma única instância do banco para o app inteiro (ver B7)
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "database-name"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true) // recria o DB se o esquema mudar
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
