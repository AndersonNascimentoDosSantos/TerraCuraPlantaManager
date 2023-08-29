package br.com.andersonssales.terracuraplantamanager

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2


import coil.compose.rememberImagePainter
import coil.load
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONObject

@Composable
fun PlantIdentificationScreen(viewModel: PlantIdentificationViewModel) {
    val plantInfo = viewModel.plantInfo.value



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        plantInfo?.let { info ->
            val imageUri = info.optString("image")

            val suggestions = info.getJSONObject("result").getJSONObject("classification").getJSONArray("suggestions")
            val suggestionImages = mutableListOf<String>()

            if (suggestions != null) {
                for (i in 0 until suggestions.length()) {
                    val suggestion = suggestions.getJSONObject(i)
                    val similarImages = suggestion.optJSONArray("similar_images")

                    if (similarImages != null && similarImages.length() > 0) {
                        val imageUrl = similarImages.getJSONObject(0).optString("url")
                        if (imageUrl.isNotEmpty()) {
                            Log.e("if imageurl", "item no imageUrl")
                            suggestionImages.add(imageUrl)
                        }
                    }else{
                        Log.e("imageUrl:","nao tem imagem similar")
                    }
                }
            }else{
                Log.e("imageUrl:","nao tem sugestions")
            }

            if (suggestionImages.isNotEmpty()) {
                ImageSlider(images = suggestionImages)
            }

            Text(text = info.optString("common_names", "No common names available"))
            Text(text = info.optString("description", "No description available"))


        } ?: run {
            Text(text = "Aguardando informações...")
        }

    }

}

@SuppressLint("RememberReturnType")
@Composable
fun ImageSlider(images: List<String>) {
    val context = LocalContext.current
    val viewPager = remember { ViewPager2(context) }
//    val pagerState = rememberPagerState(pageCount = images.size)

    AndroidView(
        factory = { viewPager },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // Ajuste a altura conforme necessário
    ) { view ->
        view.adapter = ViewPagerAdapter(images = images)
        TabLayout(view.context).apply {
            TabLayoutMediator(this, viewPager) { _, _ -> }.attach()
        }
    }
}

class ViewPagerAdapter(private val images: List<String>) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageView = holder.itemView as ImageView
        imageView.load(images[position]) // Use uma biblioteca de carregamento de imagens aqui
    }

    override fun getItemCount(): Int = images.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

