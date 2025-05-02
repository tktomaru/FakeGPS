package jp.tukutano.fakegps.ui.dashboard

import android.content.Context
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import jp.tukutano.fakegps.R
import jp.tukutano.fakegps.service.FakeLocationService

class DashboardFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null
    private var previewMarker: Marker? = null    // 設定予定ピン


    // SharedPreferences に座標を保存／読み込み
    private val prefs by lazy {
        requireContext().getSharedPreferences("fakegps_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 地図フラグメント取得
        val mapFrag = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)

        // ボタンは onMapReady でリスナーをセット
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 初期カメラ移動（東京駅）
        val tokyo = LatLng(35.681236, 139.767125)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 12f))

        // 1) 保存済みの座標があればピンを立てる
        val savedLat = prefs.getString("mock_lat", null)?.toDoubleOrNull()
        val savedLng = prefs.getString("mock_lng", null)?.toDoubleOrNull()
        if (savedLat != null && savedLng != null) {
            val pos = LatLng(savedLat, savedLng)
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Mock Location\n${"%.5f".format(savedLat)}, ${"%.5f".format(savedLng)}")
            )
        }

        // 2) プレビュー用ピンを初期化（カメラ中心）
        val center0 = map.cameraPosition.target
        previewMarker = map.addMarker(
            MarkerOptions()
                .position(center0)
                .alpha(0.6f)
                .title("Preview\n${"%.5f".format(center0.latitude)}, ${"%.5f".format(center0.longitude)}")
        )

        // 3) カメラ移動後にプレビュー用ピンを更新
        map.setOnCameraIdleListener {
            val center = map.cameraPosition.target
            previewMarker?.position = center
            previewMarker?.title =
                "Preview\n${"%.5f".format(center.latitude)}, ${"%.5f".format(center.longitude)}"
        }

        // 2) ボタンリスナー
        view?.findViewById<Button>(R.id.btnSetMock)?.setOnClickListener {
            val center = map.cameraPosition.target

            // サービスに注入指示
            requireContext().startForegroundService(
                Intent(requireContext(), FakeLocationService::class.java).apply {
                    putExtra(FakeLocationService.EXTRA_LAT, center.latitude)
                    putExtra(FakeLocationService.EXTRA_LNG, center.longitude)
                }
            )

            // SharedPreferences に保存
            prefs.edit()
                .putString("mock_lat", center.latitude.toString())
                .putString("mock_lng", center.longitude.toString())
                .apply()

            // 既存ピンを消して新ピン
            currentMarker?.remove()
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(center)
                    .title("Mock Location\n${"%.5f".format(center.latitude)}, ${"%.5f".format(center.longitude)}")
            )

            Toast.makeText(
                requireContext(),
                "Mock location set: ${"%.5f".format(center.latitude)}, ${"%.5f".format(center.longitude)}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}