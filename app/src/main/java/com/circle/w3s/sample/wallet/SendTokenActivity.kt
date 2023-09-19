package com.circle.w3s.sample.wallet

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.circle.w3s.sample.wallet.databinding.SendpageBinding
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.google.gson.Gson
import android.content.Intent
import java.util.UUID
import com.circle.w3s.sample.wallet.ui.main.LoadingDialog

data class ChallengeResponse(
    val data: ChallengeData
)

data class ChallengeData(
    val challengeId: String
)


class SendTokenActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val binding = SendpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipientWalletAddressUserInput = binding.recipientInputEditText
        val tokenAmountUserInput = binding.tokenAmountEditText
        val sendButton = binding.sendTransferButton
        val backButton = binding.backButton

        // Retrieve apiKey and userId from the intent extras
        val apiKey = intent.getStringExtra("apiKey")
        val userToken = intent.getStringExtra("userToken")
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val tokenId = intent.getStringExtra("tokenId")
        val walletId = intent.getStringExtra("walletId")


        Log.d("SendTokenActivity", "Msg: ${userToken},${apiKey}, $encryptionKey, $tokenId, $walletId")

        backButton.setOnClickListener {
            //redirect to send tokens page
            val intent = Intent(this@SendTokenActivity, HomePageActivity::class.java)

            //pass data to next page
            intent.putExtra("apiKey", apiKey)
            intent.putExtra("userToken", userToken)
            intent.putExtra("encryptionKey", encryptionKey)

            // Start the new activity
            startActivity(intent)

            // Finish the current activity if needed
            finish()
        }

        sendButton.setOnClickListener {
            Log.d("SendTokenActivity", "On Send button click.")

            //validate input fields
            val recipientWalletAddress = recipientWalletAddressUserInput.text.toString().trim()
            if (recipientWalletAddress.isEmpty()) {
                // userId is empty, display a warning message
                recipientWalletAddressUserInput.error = "Recipient wallet address is required."
            }

            val tokenAmountInput = tokenAmountUserInput.text.toString().trim()
            if (tokenAmountInput.isEmpty()) {
                // userId is empty, display a warning message
                tokenAmountUserInput.error = "Token amount is required."
            }

            if(recipientWalletAddressUserInput.error == null && tokenAmountUserInput.error == null){
                val uuid = UUID.randomUUID()
                //show loading modal
                val loadingDialog = LoadingDialog(this, "Processing your request, please wait...") // Specify the loading text here
                loadingDialog.show()

                Log.d("SendTokenActivity", "NEW BODY TEST")
                //api call
                GlobalScope.launch(Dispatchers.IO) {
                    val client = OkHttpClient()

                    val mediaType = "application/json".toMediaTypeOrNull()
                    val body = "{\"amounts\":[\"$tokenAmountInput\"],\"destinationAddress\":\"${recipientWalletAddress}\",\"idempotencyKey\":\"${uuid}\",\"feeLevel\":\"MEDIUM\",\"tokenId\":\"${tokenId}\",\"walletId\":\"${walletId}\"}".toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://api.circle.com/v1/w3s/user/transactions/transfer")
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("X-User-Token", "$userToken")
                        .addHeader("content-type", "application/json")
                        .addHeader("authorization", "Bearer $apiKey")
                        .build()

                    try {
                        val response = client.newCall(request).execute()

                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            // Use Gson to parse the JSON response into your data class
                            val gson = Gson()
                            val responseObject = gson.fromJson(responseBody,ChallengeResponse::class.java)

                            val challengeIdResponse = responseObject.data.challengeId
                            Log.e("SendTokenActivity", "Data: $challengeIdResponse")
                            runOnUiThread {
                                loadingDialog.dismiss()
                                //redirect to main fragment

                                val intent = Intent(this@SendTokenActivity, MainActivity::class.java)

                                //pass data to next page, user only needs to input APP ID
                                intent.putExtra("apiKey", apiKey)
                                intent.putExtra("userToken", userToken)
                                intent.putExtra("encryptionKey", encryptionKey)
                                intent.putExtra("challengeId", challengeIdResponse)

                                // Start the new activity
                                startActivity(intent)

                                // Finish the current activity if needed
                                finish()
                            }

                        } else {
                            // Update UI components
                            runOnUiThread {
                                Log.e("SendTokenActivity", "Error: $response")
                                loadingDialog.dismiss()
                                //show toast message
                                Toast.makeText(this@SendTokenActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                                 }

                            }


                    }  catch (e: IOException) {
                        Log.e("SendTokenActivity", "Error: ${e.message}", e)
                    }

                }
            }

        }

    }
}