package com.lakelobster.trainerspin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lakelobster.trainerspin.databinding.DeviceListItemBinding
import com.lakelobster.trainerspin.databinding.FragmentListDevicesBinding
import com.lakelobster.trainerspin.util.RecyclerObservableListAdapter
import kotlinx.android.synthetic.main.fragment_list_devices.*
import java.lang.Exception

class DeviceListFragment : Fragment() {

    private lateinit var deviceListViewAdapter: RecyclerObservableListAdapter<BtScanHolder, DeviceListItemBinding>
    private lateinit var deviceListViewManager: RecyclerView.LayoutManager


    private lateinit var viewModel: TacxViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel =
            ViewModelProvider({ activity?.viewModelStore!! }).get(TacxViewModel::class.java)
                ?: throw Exception("Invalid activity!")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding : FragmentListDevicesBinding = DataBindingUtil.inflate(inflater,R.layout.fragment_list_devices, container,false)
        binding.viewmodel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceListViewManager = LinearLayoutManager(context)
        //deviceListViewAdapter = DeviceListAdapter(viewModel.scannedDevices)
        deviceListViewAdapter = RecyclerObservableListAdapter<BtScanHolder,DeviceListItemBinding>(viewModel.scannedDevices,
            { i -> BR.device },
            { i -> R.layout.device_list_item})

        deviceListViewAdapter.onItemClickListener = {
            viewModel.selectDevice(it)
            viewModel.next()
        }
        deviceListRecyclerView.adapter = deviceListViewAdapter

        deviceListRecyclerView.apply {
            setHasFixedSize(true)

            adapter = deviceListViewAdapter
            layoutManager = deviceListViewManager

        }

        deviceListRecyclerView.itemAnimator?.addDuration = 20
    }
}