package jp.tukutano.fakegps.ui.dashboard

import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import jp.tukutano.fakegps.R
import jp.tukutano.fakegps.service.FakeLocationService

class DashboardFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locMgr: LocationManager
    private val provider = LocationManager.GPS_PROVIDER

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) ボタンを取得して最初は無効化
        val btnSetMock = view.findViewById<Button>(R.id.btnSetMock)
        btnSetMock.isEnabled = false

        // LocationManager とテストプロバイダー登録 は Service へ移した場合は不要

        // 2) 地図フラグメント取得＆非同期初期化
        val mapFrag = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)

        // クリックリスナーは onMapReady のあとにセットする
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 初期カメラ移動
        val tokyo = LatLng(/* latitude = */ 35.681236, /* longitude = */ 139.767125)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 12f))

        // 3) ボタンを有効化してクリック処理を設定
        view?.findViewById<Button>(R.id.btnSetMock)?.apply {
            isEnabled = true
            setOnClickListener {
                val center = map.cameraPosition.target
                // Service に送る or 直接 setMockLocation など
                requireContext().startService(
                    Intent(requireContext(), FakeLocationService::class.java).apply {
                        putExtra(FakeLocationService.EXTRA_LAT, center.latitude)
                        putExtra(FakeLocationService.EXTRA_LNG, center.longitude)
                    }
                )
                Toast.makeText(
                    requireContext(),
                    "Mock location: ${center.latitude}, ${center.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}