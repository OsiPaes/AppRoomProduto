package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var productDao: ProductDao
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Banco de dados Room (Singleton, sem allowMainThreadQueries)
        productDao = AppDatabase.getInstance(this).productDao()

        // A3 (desafio extra): lista na tela com RecyclerView
        adapter = ProductAdapter { product -> excluirProduto(product) }
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = adapter

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarPorNome() }
        binding.btnExcluirTodos.setOnClickListener { excluirTodos() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
    }

    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val price = binding.editProductPrice.text.toString().toDoubleOrNull()
        val description = binding.editProductDescription.text.toString()
        val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0

        if (name.isEmpty()) {
            binding.editProductName.error = "Informe o nome"
            return
        }
        // A2: não permite preço vazio, zero ou negativo
        if (price == null || price <= 0) {
            binding.editProductPrice.error = "Informe um preço válido"
            return
        }

        val product = Product(name = name, price = price, description = description, quantity = quantity)

        // Insere o produto no banco de forma assíncrona (corrotina)
        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i("TAG", "Produto inserido: $name - R$$price")
            Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
            limparCampos()
            listarProdutos()
        }
    }

    // A1: limpa os três campos e devolve o cursor para o Nome
    private fun limparCampos() {
        binding.editProductName.text.clear()
        binding.editProductPrice.text.clear()
        binding.editProductDescription.text.clear()
        binding.editProductQuantity.text.clear()
        binding.editProductName.requestFocus()
    }

    private fun listarProdutos() {
        lifecycleScope.launch {
            val products = productDao.getAll()
            products.forEach {
                Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}")
            }
            mostrarProdutos(products)
        }
    }

    // A3 + A4: mostra a lista na tela e o total do estoque
    private fun mostrarProdutos(products: List<Product>) {
        adapter.submitList(products)
        binding.textEmpty.text = if (products.isEmpty()) "Nenhum produto encontrado." else ""
        val total = products.sumOf { it.price }
        binding.textTotal.text = "Produtos: ${products.size} | Soma dos preços: R$ %.2f".format(total)
    }

    // A5: busca por nome com LIKE "%texto%"
    private fun buscarPorNome() {
        val texto = binding.editBusca.text.toString().trim()
        lifecycleScope.launch {
            val products = productDao.buscarPorNome("%$texto%")
            Log.i("TAG", "Busca por \"$texto\": ${products.size} resultado(s)")
            mostrarProdutos(products)
        }
    }

    // A6: exclui todos os produtos
    private fun excluirTodos() {
        lifecycleScope.launch {
            productDao.deleteAll()
            Log.i("TAG", "Todos os produtos foram excluídos")
            Toast.makeText(this@MainActivity, "Todos os produtos excluídos!", Toast.LENGTH_SHORT).show()
            listarProdutos()
        }
    }

    // A6 (desafio extra): exclui um produto específico com @Delete
    private fun excluirProduto(product: Product) {
        lifecycleScope.launch {
            productDao.delete(product)
            Log.i("TAG", "Produto excluído: id=${product.id} ${product.name}")
            Toast.makeText(this@MainActivity, "${product.name} excluído!", Toast.LENGTH_SHORT).show()
            listarProdutos()
        }
    }

    // A7: altera o preço de um produto já cadastrado usando @Update
    private fun atualizarPreco() {
        val id = binding.editUpdateId.text.toString().toIntOrNull()
        val novoPreco = binding.editUpdatePrice.text.toString().toDoubleOrNull()

        if (id == null) {
            binding.editUpdateId.error = "Informe o id"
            return
        }
        if (novoPreco == null || novoPreco <= 0) {
            binding.editUpdatePrice.error = "Informe um preço válido"
            return
        }

        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                binding.editUpdateId.error = "Produto não encontrado"
                return@launch
            }
            productDao.update(product.copy(price = novoPreco))
            Log.i("TAG", "Preço atualizado: ${product.name} R$${product.price} -> R$$novoPreco")
            Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
            binding.editUpdateId.text.clear()
            binding.editUpdatePrice.text.clear()
            listarProdutos()
        }
    }
}
