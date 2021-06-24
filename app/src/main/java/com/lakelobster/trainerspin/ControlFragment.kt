package com.lakelobster.trainerspin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lakelobster.trainerspin.databinding.FragmentControlBinding
import org.w3c.dom.Text
import java.lang.Exception

class ControlFragment : Fragment() {

    private lateinit var viewModel: TacxViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider({ activity?.viewModelStore!! }).get(TacxViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentControlBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_control, container, false)
        binding.viewmodel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val zeroButton = view.findViewById<Button>(R.id.zeroButton)
        zeroButton.setOnClickListener {
            viewModel.slope10x.set(0)
        }
    }

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("Slope10x2ProgressString")
        fun slope10x2ProgressString(textView: TextView, value: Int) {

            textView.text = "%.1f %%".format(value / 10.0)
        }
    }


}