package `in`.co.xyon.application.android.module.deviceconfig.ui

import `in`.co.xyon.application.android.module.deviceconfig.databinding.ActivityMainBinding
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.ui.fragment.DeviceTypeSelectionFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: WifiOnlyViewModel by viewModels<WifiOnlyViewModel>()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        //do nothing on backpressed
        if (supportFragmentManager.fragments.last()?.childFragmentManager?.fragments?.get(0) is DeviceTypeSelectionFragment)
            super.onBackPressed()

    }
}