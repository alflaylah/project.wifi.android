package com.app.al.wifi.util

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.support.v4.app.FragmentActivity
import com.app.al.wifi.const.ApplicationConst
import com.app.al.wifi.const.ApplicationConst.SECURITY_TYPE
import com.app.al.wifi.const.ApplicationConst.SECURITY_TYPE.SECURITY_PSK
import com.app.al.wifi.const.ApplicationConst.SECURITY_TYPE.SECURITY_WEP

/**
 * WIFIユーティリティ
 */
object WifiUtils {

  /**
   * 接続可能状態に設定
   *
   * @param context context
   * @return true：WIFI接続可能 false：WIFI接続不可
   */
  fun enable(context: Context) {
    val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
    wifiManager.isWifiEnabled = true
  }

  /**
   * 接続の状態を返却する
   *
   * @param context context
   * @return true：接続中 false：未接続
   */
  fun isConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
  }

  /**
   * 接続
   *
   * @param context context
   * @param ssid SSID
   * @param capabilities セキュリティ情報
   * @param password パスワード
   */
  fun connect(context: Context, ssid: String, capabilities: String, password: String) {
    val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
    // SSIDが端末に登録済みか判定
    val targetWifiConfiguration: WifiConfiguration? = wifiManager.configuredNetworks.lastOrNull { it.SSID.contains(ssid) }
    if (targetWifiConfiguration != null) {
      // 登録済
      connect(context, targetWifiConfiguration.networkId)
    } else {
      // 未登録
      // 登録します
      val networkId = wifiManager.addNetwork(getWifiConfiguration(ssid, capabilities, password))
      if (networkId != -1) {
        // 登録成功
        // 接続先を切断して新たに接続
        connect(context, networkId)
      } else {
        // 登録失敗
      }
    }
  }

  /**
   * 接続
   *
   * @param context context
   * @param networkId ネットワークID
   */
  private fun connect(context: Context, networkId: Int) {
    disconnect(context)
    (context.getSystemService(WIFI_SERVICE) as WifiManager).enableNetwork(networkId, true)
  }

  /**
   * 切断
   *
   * @param context context
   */
  private fun disconnect(context: Context) {
    val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
    wifiManager.configuredNetworks.forEach { wifiManager.enableNetwork(it.networkId, false) }
  }

  /**
   * 接続設定返却
   *
   * @param ssid SSID
   * @param capabilities セキュリティ情報
   * @param password パスワード
   * @return WIFI接続設定
   */
  private fun getWifiConfiguration(ssid: String, capabilities: String, password: String): WifiConfiguration? {
    val wifiConfiguration = WifiConfiguration()
    val securityType = getSecurity(capabilities)
    when (securityType) {
      SECURITY_TYPE.SECURITY_NONE -> wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
      SECURITY_TYPE.SECURITY_WEP -> {
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
        wifiConfiguration.wepKeys[0] = getPassword(securityType, password)
      }
      SECURITY_TYPE.SECURITY_PSK -> {
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        wifiConfiguration.preSharedKey = getPassword(securityType, password)
      }
    }
    wifiConfiguration.SSID = ApplicationConst.DOUBLE_QUOTE + ssid + ApplicationConst.DOUBLE_QUOTE
    wifiConfiguration.hiddenSSID = false
    return wifiConfiguration
  }

  /**
   * セキュリティ情報返却
   *
   * @return セキュリティ情報
   */
  private fun getSecurity(capabilities: String): SECURITY_TYPE = when {
    capabilities.contains(SECURITY_TYPE.SECURITY_WEP.securityType) -> SECURITY_TYPE.SECURITY_WEP
    capabilities.contains(SECURITY_TYPE.SECURITY_PSK.securityType) -> SECURITY_TYPE.SECURITY_PSK
    else -> SECURITY_TYPE.SECURITY_NONE
  }

  /**
   * パスワード返却
   *
   * @param securityType セキュリティ情報
   * @param password パスワード
   * @return セキュリティ情報
   */
  private fun getPassword(securityType: SECURITY_TYPE, password: String): String = when (securityType) {
    SECURITY_WEP -> {
      if ((password.length == 10 || password.length == 26) && password.matches(ApplicationConst.REGEX_WEP.toRegex())) {
        password
      } else {
        StringUtils.getFormatDoubleQuote(password)
      }
    }
    SECURITY_PSK -> {
      if (password.matches(ApplicationConst.REGEX_PSK.toRegex())) {
        password
      } else {
        StringUtils.getFormatDoubleQuote(password)
      }
    }
    else -> {
      ApplicationConst.EMPTY
    }
  }

  /**
   * 検索結果一覧返却
   *
   * @param activity Activity
   * @return 検索結果一覧
   */
  fun getWifiInformationList(activity: FragmentActivity): List<ScanResult> {
    val wifiManager = activity.getSystemService(WIFI_SERVICE) as WifiManager
    var scanResults = listOf<ScanResult>()
    if (wifiManager.startScan()) {
      // SSIDが空でない情報のみ抽出
      scanResults = wifiManager.scanResults.filter { it.SSID.isNotEmpty() }
    }
    // SSID、BSSID順にソートした状態で返却
    return scanResults.sortedWith(compareBy({ it.SSID }, { it.BSSID }))
  }
}