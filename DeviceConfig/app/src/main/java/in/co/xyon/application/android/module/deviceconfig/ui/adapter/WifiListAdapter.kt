package `in`.co.xyon.application.android.module.deviceconfig.ui.adapter

import `in`.co.xyon.application.android.module.deviceconfig.R
import `in`.co.xyon.application.android.module.deviceconfig.databinding.ItemWifiAccessPointBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.WiFiAccessPoint
import timber.log.Timber
import javax.inject.Inject

class WifiListAdapter(
    var wifiApList: List<WiFiAccessPoint>,
    private val clickListener: (WiFiAccessPoint) -> Unit
) : RecyclerView.Adapter<WifiListViewHolder>() {

    // create an inner class with name ViewHolder (inner class might lead to leak in memory)
    // It takes a view argument, in which pass the generated class of single_item.xml
    // ie ItemWifiAccessPointBinding and in the RecyclerView.ViewHolder(binding.root) pass it like this
    /*class ViewHolder(val binding: ItemWifiAccessPointBinding)
        : RecyclerView.ViewHolder(binding.root)*/

    // inside the onCreateViewHolder inflate the view of ItemWifiAccessPoint
    // and return new ViewHolder object containing this layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWifiAccessPointBinding.inflate(inflater, parent, false)
        return WifiListViewHolder(binding){
            clickListener(wifiApList[it])
        }
    }

    // bind the items with each item of the list wifiApList
    // which than will be shown in recycler view
    override fun onBindViewHolder(holder: WifiListViewHolder, position: Int) {
        holder.bind(wifiApList[position])
        /*if (wifiApList[position].wifiName == holder.itemView.context.resources.getString(R.string.join_other_network)){
            holder.itemView.setBackgroundColor(holder.itemView.context.resources.getColor(R.color.purple_200))
        }*/
    }

    // return the size of wifiApList
    override fun getItemCount(): Int {
        return wifiApList.size
    }
}
