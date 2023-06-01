package com.tinyversespace.mtvapp.views

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.activities.FullScreenActivity
import com.tinyversespace.mtvapp.utils.PhotoItem


class PhotoPreviewAdapter() : ListAdapter<PhotoItem, PhotoPreviewAdapter.PhotoPreviewViewHolder>(
    PhotoPreviewDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoPreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_preview, parent, false)
        return PhotoPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoPreviewViewHolder, position: Int) {
        val photoItem = getItem(position)
        holder.bind(photoItem)
    }


    inner class PhotoPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val statusImageView: ImageView = itemView.findViewById(R.id.statusImageView)

        fun bind(photoItem: PhotoItem) {
            Glide.with(itemView)
                .load(photoItem.photoUri)
                .into(imageView)
            // 检查照片是否OK，并设置statusImageView的可见性和图像资源
            val isPhotoOK = photoItem.status
            if (isPhotoOK) {
                statusImageView.visibility = View.VISIBLE
                statusImageView.setImageResource(R.drawable.ic_check)
            } else {
                statusImageView.visibility = View.VISIBLE
                statusImageView.setImageResource(R.drawable.ic_cross)
            }

            // 设置点击事件
            imageView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val imageUri = photoItem.photoUri
                    // 启动全屏展示的 Activity
                    val intent = Intent(itemView.context, FullScreenActivity::class.java)
                    intent.putExtra(FullScreenActivity.EXTRA_IMAGE_URI, imageUri.toString())
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

    class PhotoPreviewDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
        override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem == newItem
        }
    }
}
