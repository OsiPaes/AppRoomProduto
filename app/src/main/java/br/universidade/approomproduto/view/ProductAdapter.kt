package br.universidade.approomproduto.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.universidade.approomproduto.databinding.ItemProductBinding
import br.universidade.approomproduto.model.Product

class ProductAdapter(
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var products: List<Product> = emptyList()

    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitList(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            textName.text = "#${product.id} - ${product.name}"
            textPrice.text = "R$ %.2f".format(product.price)
            textQuantity.text = "Qtd: ${product.quantity}"
            textDescription.text = product.description.orEmpty()
            btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    override fun getItemCount() = products.size
}
