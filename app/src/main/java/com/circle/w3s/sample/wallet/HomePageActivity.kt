package com.circle.w3s.sample.wallet

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.circle.w3s.sample.wallet.databinding.HomepageBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


data class GetUserWalletResponse(
    val data: WalletData
)

data class WalletData(
    val wallets: List<Wallet>
)

data class Wallet(
    val id: String,
    val state: String,
    val walletSetId: String,
    val custodyType: String,
    val userId: String,
    val address: String,
    val blockchain: String,
    val accountType: String,
    val updateDate: String,
    val createDate: String
)

data class TokenBalanceResponse(
    val data: TokenBalanceData
)

data class TokenBalanceData(
    val tokenBalances: List<TokenBalance>
)

data class TokenBalance(
    val token: TokenInfo,
    val amount: String,
    val updateDate: String
)

data class TokenInfo(
    val id: String,
    val blockchain: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val isNative: Boolean,
    val updateDate: String,
    val createDate: String
)

class HomePageActivity : AppCompatActivity() {

    // Values to retrieve from the API
    private var userWalletId = ""
    private var userWalletAddress = ""
    private var userTokenBalance = ""
    private var userTokenSymbol = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = HomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        val statusLoadingTextView = binding.statusLoadingTextView
        val tokenBalanceText = binding.tokenBalanceText
        val progressBar = binding.progressBar2
        val refreshButton = binding.refreshbutton
        val receiveButton = binding.receiveBtn
        val sendButton = binding.SendBtn
        val viewTransactions = binding.viewTransactionBtn

        // Retrieve values from the intent extras
        val apiKey = intent.getStringExtra("apiKey")
        val userToken = intent.getStringExtra("userToken")

        // Delay before making network requests (5 seconds)
        val delayMilliseconds = 5000L

        // Start the network request to get user wallet id
        getUserWalletId(apiKey, userToken, progressBar, statusLoadingTextView, tokenBalanceText, delayMilliseconds)

        refreshButton.setOnClickListener{
            Log.d("HomePageActivity", "On Refresh button press")
            statusLoadingTextView.text = "Loading....Getting wallet data"
            progressBar.visibility = View.VISIBLE
            // Delay before making network requests (2 seconds)
            val delayInMilliseconds = 2000L
            if (userWalletId.isNotEmpty()){
                getUserWalletId(apiKey, userToken, progressBar, statusLoadingTextView, tokenBalanceText, delayInMilliseconds)
            }

        }
    }

    private fun getUserTokenBalance(
        apiKey: String?,
        userToken: String?,
        progressBar: ProgressBar,
        tokenBalanceText: TextView,
        statusLoadingTextView: TextView
    ) {
        Log.d("HomePageActivity", "Getting Token Balance: $userToken, $userWalletId")
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.circle.com/v1/w3s/wallets/$userWalletId/balances?pageSize=10")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("X-User-Token", "$userToken")
                .addHeader("authorization", "Bearer $apiKey")
                .build()

            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    // Use Gson to parse the JSON response into your data class
                    val gson = Gson()
                    val responseObject = gson.fromJson(responseBody, TokenBalanceResponse::class.java)
                    val tokenBalanceArrayData = responseObject.data.tokenBalances
                    //NOTE: when creating wallet for first time, user does not have any token balance
                    //calling the get token balance endpoint will return empty array, only when
                    //user wallet has some balance tokens will the data be returned.

                    if (tokenBalanceArrayData.isNotEmpty()) {
                        // Assuming array element 1 is AVAX FUJI token
                        val firstWalletTokenData = tokenBalanceArrayData[0]
                        userTokenBalance = firstWalletTokenData.amount
                        userTokenSymbol = firstWalletTokenData.token.symbol

                        // Update UI components
                        runOnUiThread {
                            statusLoadingTextView.text = "Success!\nWallet Address: $userWalletAddress"
                            tokenBalanceText.text = "$userTokenSymbol: $userTokenBalance"
                            progressBar.visibility = View.INVISIBLE
                        }
                    } else {
                        // Update UI components
                        runOnUiThread {
                            statusLoadingTextView.text = "Success!\nYou have no tokens in your wallet, send some AVAX-Fuji tokens into your wallet address at $userWalletAddress"
//                            tokenBalanceText.text = "$userTokenSymbol: $userTokenBalance"
                            progressBar.visibility = View.INVISIBLE
                        }
                        Log.d("HomePageActivity", "No Token Balances data found")
                    }
                } else {
                    // Handle API response error
                    Log.e("HomePageActivity", "Error ${response.code}")
                    runOnUiThread {
                        progressBar.visibility = View.INVISIBLE
                    }
                }
            } catch (e: IOException) {
                Log.e("HomePageActivity", "Get Wallets Error: ${e.message}", e)
            }
        }
    }

    private fun getUserWalletId(
        apiKey: String?,
        userToken: String?,
        progressBar: ProgressBar,
        statusLoadingTextView: TextView,
        tokenBalanceText: TextView,
        delayMilliseconds: Long
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            delay(delayMilliseconds)

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.circle.com/v1/w3s/wallets")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("X-User-Token", "$userToken")
                .addHeader("authorization", "Bearer $apiKey")
                .build()

            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    // Use Gson to parse the JSON response into your data class
                    val gson = Gson()
                    val responseObject = gson.fromJson(responseBody, GetUserWalletResponse::class.java)

                    val myWalletObjectArray = responseObject.data.wallets

                    if (myWalletObjectArray.isNotEmpty()) {
                        val firstWallet = myWalletObjectArray[0]
                        userWalletId = firstWallet.id
                        userWalletAddress = firstWallet.address

                        // Call the function to get user token balance
                        getUserTokenBalance(apiKey, userToken, progressBar, tokenBalanceText, statusLoadingTextView)
                    } else {
                        // Handle the case when the array is empty
                        Log.e("HomePageActivity", "No Wallets found for user.")
                    }
                } else {
                    // Handle error response
                    Log.e("HomePageActivity", "Error ${response.code}")
                    runOnUiThread {
                        progressBar.visibility = View.INVISIBLE
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.INVISIBLE
                    Log.e("HomePageActivity", "Get Wallets Error: ${e.message}", e)
                }
            }
        }
    }


}
