package `in`.co.xyon.application.android.module.deviceconfig.ui.adapter

import `in`.co.xyon.application.android.module.deviceconfig.databinding.ItemWifiAccessPointBinding
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.WiFiAccessPoint

class WifiListViewHolder(
    private val binding: ItemWifiAccessPointBinding,
    private val clickListener: (Int) -> Unit
): RecyclerView.ViewHolder(binding.root){

    init {
        binding.root.setOnClickListener{
            clickListener(adapterPosition)
        }
    }

    fun bind( wifiAp : WiFiAccessPoint){//, clickListener: (WiFiAccessPoint) -> Unit){

            binding.tvWifiName.text = wifiAp.wifiName
            //if (this.rssi == -1000) binding.ivWifiRssi.visibility = View.GONE
            //else
                binding.ivWifiRssi.setImageLevel(getRssiLevel(wifiAp.rssi))
            if(wifiAp.security == ESPConstants.WIFI_OPEN.toInt())
                binding.ivWifiSecurity.visibility = View.GONE
            else binding.ivWifiSecurity.visibility = View.VISIBLE

            //binding.root.setOnClickListener { clickListener(wifiAp) }

    }

    private fun getRssiLevel(rssiValue: Int): Int {
        return when{
            rssiValue >= -50 -> 3
            rssiValue in -60..-50  -> 2
            rssiValue in -67..-60 -> 1
            else -> 0
        }
    }
}