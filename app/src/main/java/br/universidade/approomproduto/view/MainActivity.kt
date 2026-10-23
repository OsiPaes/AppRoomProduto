package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.R
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration(dropAllTables = true) // recria o banco se o schema mudar
            .build()

        productDao = db.productDao()

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarPorNome() }
        binding.btnExcluirTodos.setOnClickListener { excluirTodos() }
        binding.btnExcluirPorId.setOnClickListener { excluirPorId() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
    }

    // A2: valida nome e preço antes de inserir
    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val price = binding.editProductPrice.text.toString().toDoubleOrNull()
        val description = binding.editProductDescription.text.toString()
        val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0

        if (name.isEmpty()) {
            binding.editProductName.error = getString(R.string.erro_nome_vazio)
            return
        }

        if (price == null || price <= 0.0) {
            binding.editProductPrice.error = getString(R.string.erro_preco_invalido)
            binding.editProductPrice.requestFocus()
            return
        }

        val product = Product(
            name = name,
            price = price,
            description = description,
            quantity = quantity
        )

        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i(TAG, "Produto inserido: $name - R$$price")
            mostrarToast(getString(R.string.msg_produto_inserido))
            limparCampos()
        }
    }

    // A1: limpa os campos e volta o cursor para o Nome
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
                Log.i(TAG, "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}")
            }
            mostrarNaTela(products)
        }
    }

    // A3 e A4: mostra a lista no TextView com a quantidade e a soma dos preços
    private fun mostrarNaTela(products: List<Product>) {
        if (products.isEmpty()) {
            binding.txtResumo.text = getString(R.string.msg_nenhum_produto)
            binding.txtListaProdutos.text = ""
            return
        }

        val total = products.sumOf { it.price }
        binding.txtResumo.text =
            getString(R.string.resumo_produtos, products.size, formatarReais(total))

        binding.txtListaProdutos.text = products.joinToString("\n\n") {
            getString(
                R.string.item_produto,
                it.id,
                it.name,
                formatarReais(it.price),
                it.quantity,
                it.description.orEmpty().ifEmpty { "-" }
            )
        }
    }

    private fun formatarReais(valor: Double): String =
        "R$ " + String.format(LOCALE_BR, "%.2f", valor)

    private fun mostrarToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    // A5: busca por nome usando LIKE "%texto%"
    private fun buscarPorNome() {
        val texto = binding.editBusca.text.toString().trim()
        lifecycleScope.launch {
            val resultado = productDao.buscarPorNome("%$texto%")
            mostrarNaTela(resultado)
        }
    }

    // A6: exclui todos os produtos
    private fun excluirTodos() {
        lifecycleScope.launch {
            productDao.deleteAll()
            mostrarToast(getString(R.string.msg_todos_excluidos))
            mostrarNaTela(emptyList())
        }
    }

    // A6 (desafio extra): exclui um produto pelo id usando @Delete
    private fun excluirPorId() {
        val id = binding.editId.text.toString().toIntOrNull()
        if (id == null) {
            binding.editId.error = getString(R.string.erro_id_vazio)
            return
        }

        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                mostrarToast(getString(R.string.msg_produto_nao_encontrado, id))
            } else {
                productDao.delete(product)
                mostrarToast(getString(R.string.msg_produto_excluido, id))
                mostrarNaTela(productDao.getAll())
            }
        }
    }

    // A7: atualiza o preço de um produto usando @Update
    private fun atualizarPreco() {
        val id = binding.editId.text.toString().toIntOrNull()
        val novoPreco = binding.editNovoPreco.text.toString().toDoubleOrNull()

        if (id == null) {
            binding.editId.error = getString(R.string.erro_id_vazio)
            return
        }
        if (novoPreco == null || novoPreco <= 0.0) {
            binding.editNovoPreco.error = getString(R.string.erro_preco_invalido)
            return
        }

        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                mostrarToast(getString(R.string.msg_produto_nao_encontrado, id))
            } else {
                productDao.update(product.copy(price = novoPreco))
                mostrarToast(getString(R.string.msg_preco_atualizado))
                binding.editId.text.clear()
                binding.editNovoPreco.text.clear()
                mostrarNaTela(productDao.getAll())
            }
        }
    }

    companion object {
        // mesma tag do tutorial, para filtrar no Logcat com tag:TAG
        private const val TAG = "TAG"
        private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")
    }
}
