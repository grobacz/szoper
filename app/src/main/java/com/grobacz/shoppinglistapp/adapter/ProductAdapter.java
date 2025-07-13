package com.grobacz.shoppinglistapp.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grobacz.shoppinglistapp.Product;
import com.grobacz.shoppinglistapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> 
    implements ItemTouchHelperAdapter {

    private List<Product> products;
    private OnItemClickListener listener;
    private OnStartDragListener dragListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public ProductAdapter() {
        this.products = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnStartDragListener(OnStartDragListener listener) {
        this.dragListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product currentProduct = products.get(position);
        holder.bind(currentProduct);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    public List<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        products.add(product);
        notifyItemInserted(products.size() - 1);
    }

    public void updateProduct(int position, Product product) {
        products.set(position, product);
        notifyItemChanged(position);
    }

    public void removeProduct(int position) {
        products.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(products, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(products, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        products.remove(position);
        notifyItemRemoved(position);
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtProductName;
        private final TextView txtQuantity;
        private final ImageView dragHandle;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            dragHandle = itemView.findViewById(R.id.drag_handle);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position);
                }
            });

            // Set up drag handle touch listener
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (dragListener != null) {
                        dragListener.onStartDrag(this);
                    }
                    return true;
                }
                return false;
            });
        }

        public void bind(Product product) {
            txtProductName.setText(product.getName());
            // Quantity display removed as Product class doesn't have getQuantity() method
        }
    }
}

interface ItemTouchHelperAdapter {
    boolean onItemMove(int fromPosition, int toPosition);
    void onItemDismiss(int position);
}
