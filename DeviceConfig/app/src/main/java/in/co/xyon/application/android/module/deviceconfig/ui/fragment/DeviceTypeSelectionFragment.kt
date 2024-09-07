package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment


import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentDeviceTypeSelectionBinding
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.DeviceType
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.utils.collectLatestLifecycleFlow
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceTypeSelectionFragment : Fragment(){

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()

    private var _binding: FragmentDeviceTypeSelectionBinding? = null
    private val binding get() = _binding!!

    private var _arrayAdapter: ArrayAdapter<*>? = null
    private val arrayAdapter get() = _arrayAdapter!!
    private var devTypeList: MutableList<String> = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Timber.d("onCreateView...")
        _binding = FragmentDeviceTypeSelectionBinding.inflate(inflater)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // in here you can do logic when backPress is clicked
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _navController = Navigation.findNavController(view)

        observeState()
        initView()

    }

    private fun initView(){
        devTypeList.clear()
        for (devType in DeviceType.values()){
            if (devType != DeviceType.NONE)
                devTypeList.add(devType.printableName!!)
        }

        _arrayAdapter = ArrayAdapter(requireContext(),
        android.R.layout.simple_list_item_checked,
        devTypeList)

        binding.simpleListView.adapter = arrayAdapter
        registerItemClickListener()

        binding.btnNext.setOnClickListener {
            viewModel.resetConnectedDeviceFrag()  //to make sure the stateFlows resets before moving to the next fragment
            if(viewModel.selectedDeviceType.value != DeviceType.NONE){
                val action = DeviceTypeSelectionFragmentDirections
                    .actionDevTypSelFragToConnDevFrag()
                navController.navigate(action)
            }

        }
    }

    private fun registerItemClickListener(){

        /*binding.simpleListView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Timber.d("item selected position: $position")
                viewModel.setSelectedDeviceType(DeviceType.valueOf(devTypeList[position]))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //do nothing
            }
        }*/

        binding.simpleListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                //Timber.d("item selected position: $position")
                viewModel.setSelectedDeviceType(DeviceType.valueOf(devTypeList[position]))
                binding.simpleListView.setItemChecked(position, true)
            }
    }

    private fun observeState() {
        collectLatestLifecycleFlow(viewModel.deviceTypeSelectionStateFlow){
            binding.btnNext.isEnabled = it.isDataTypeSelected
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /** required for avoiding memory leak? **/
        _navController = null
        _binding = null
        _arrayAdapter = null
    }
}