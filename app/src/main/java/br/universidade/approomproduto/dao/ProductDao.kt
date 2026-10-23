package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {
    // suspend: o Room não permite consultas na main thread
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insertAll(vararg products: Product)

    // A5: busca por nome (o chamador monta o "%texto%")
    @Query("SELECT * FROM product WHERE name LIKE :nome")
    suspend fun buscarPorNome(nome: String): List<Product>

    // A6: exclui todos os produtos
    @Query("DELETE FROM product")
    suspend fun deleteAll()

    // A6 (desafio extra): exclui um produto específico pelo id
    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Int): Product?

    // A7: atualiza um produto já cadastrado (o Room acha a linha pela chave primária)
    @Update
    suspend fun update(product: Product)
}
