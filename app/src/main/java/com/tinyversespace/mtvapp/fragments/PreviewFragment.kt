package com.tinyversespace.mtvapp.fragments

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.activities.FingerprintActivity
import com.tinyversespace.mtvapp.utils.PhotoItem
import com.tinyversespace.mtvapp.views.PhotoPreviewAdapter


class PreviewFragment : Fragment() {

    private lateinit var photoItems: ArrayList<PhotoItem>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        photoItems = arguments?.getParcelableArrayList<PhotoItem>(ARG_PHOTO_ITEMS) as ArrayList<PhotoItem>
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView : RecyclerView = view.findViewById(R.id.recyclerView)
        val  continueButton : Button = view.findViewById(R.id.continueButton)
        val  retryButton : Button = view.findViewById(R.id.retryButton)

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        val adapter = PhotoPreviewAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(photoItems)

        continueButton.setOnClickListener {
            val mainActivity = requireActivity() as FingerprintActivity
            mainActivity.onPhotoPreviewContinue()
        }

        retryButton.setOnClickListener {
            val mainActivity = requireActivity() as FingerprintActivity
            mainActivity.onPhotoPreviewRetry()
        }
    }


    companion object {
        private const val ARG_PHOTO_ITEMS = "photo_items"

        fun newInstance(photoItems: ArrayList<PhotoItem>): PreviewFragment {
            val fragment = PreviewFragment()
            val bundle = Bundle().apply {
                //putStringArray(ARG_PHOTO_ITEMS, photoItems)
                putParcelableArrayList(ARG_PHOTO_ITEMS, photoItems)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
