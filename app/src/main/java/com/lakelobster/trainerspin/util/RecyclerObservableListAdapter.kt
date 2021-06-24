package com.lakelobster.trainerspin.util
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

open class RecyclerObservableListAdapter<T,TBinding : ViewDataBinding>(private val itemList: ObservableList<T>,
                                                                       val bindingVariableFunc : (T) -> Int,
                                                                       val resourceFunc : (T) -> Int
) :
    RecyclerView.Adapter<RecyclerObservableListAdapter.ViewHolder<TBinding>>() {


    class ViewHolder<TB : ViewDataBinding>(val binding: TB) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerObservableListAdapter.ViewHolder<TBinding> {

        val inflater = LayoutInflater.from(parent.context)

        val itemView : TBinding = DataBindingUtil.inflate(inflater,
            viewType,parent,false)


        return ViewHolder(itemView)

    }

    var onItemClickListener : ((T) -> Unit)? = null
    var onItemLongClickListener : ((T) -> Boolean)? = null

    override fun getItemViewType(position: Int): Int {
        return resourceFunc(itemList[position])
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemList.addOnListChangedCallback(onListChangedCallback)
    }

    override fun onBindViewHolder(holder: ViewHolder<TBinding>, position: Int) {
        val item = itemList[position]
        holder.binding.setVariable(bindingVariableFunc?.invoke(item), item)
        //holder.bind(item)
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
        holder.binding.root.setOnLongClickListener() {
            val lcl = onItemLongClickListener
            if (lcl != null) {
                lcl.invoke(item)
            } else
                false
        }
    }

    override fun getItemCount() = itemList.size

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        itemList.removeOnListChangedCallback(onListChangedCallback)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    val onListChangedCallback = ObservableRecyclerOnListChangedCallback(this)


    class ObservableRecyclerOnListChangedCallback<T,TBinding : ViewDataBinding>(val adapter: RecyclerObservableListAdapter<T,TBinding >) : ObservableList.OnListChangedCallback<ObservableList<T>>()
    {
        override fun onChanged(sender: ObservableList<T>?) {
            adapter.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(
            sender: ObservableList<T>?,
            positionStart: Int,
            itemCount: Int
        ) {
            adapter.notifyItemRangeRemoved(positionStart,itemCount)
        }

        override fun onItemRangeMoved(
            sender: ObservableList<T>?,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            adapter.notifyItemMoved(fromPosition,toPosition)
        }

        override fun onItemRangeInserted(
            sender: ObservableList<T>?,
            positionStart: Int,
            itemCount: Int
        ) {
            adapter.notifyItemRangeInserted(positionStart,itemCount)
        }

        override fun onItemRangeChanged(
            sender: ObservableList<T>?,
            positionStart: Int,
            itemCount: Int
        ) {
            adapter.notifyItemRangeChanged(positionStart,itemCount)
        }
    }

}

