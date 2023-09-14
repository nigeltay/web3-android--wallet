package com.circle.w3s.sample.wallet

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.circle.w3s.sample.wallet.databinding.WalletcreationpageBinding
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import android.content.Intent

import android.widget.Button
import okhttp3.MediaType
import okhttp3.RequestBody

class WalletCreationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val binding = WalletcreationpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val createWalletButton = binding.createWallet
        val backButton = binding.backbutton
        val loginButton = binding.loginBtn
        val submitButton = binding.submitbutton
        val progressBar = binding.progressBar

        val apiKeyEditText = binding.apiKeyEditText
        val userIdEditText = binding.userIdEditText
        val createUserTitle = binding.createUserTitle
        val apiResponseTextView = binding.apiResponseTextView

        // Initially, hide the EditText fields and TextView
        apiKeyEditText.visibility = android.view.View.INVISIBLE
        userIdEditText.visibility = android.view.View.INVISIBLE
        createUserTitle.visibility = android.view.View.INVISIBLE
        submitButton.visibility =  android.view.View.INVISIBLE
        backButton.visibility = android.view.View.INVISIBLE
        progressBar.visibility = android.view.View.INVISIBLE
        apiResponseTextView.visibility = android.view.View.INVISIBLE

        createWalletButton.setOnClickListener {
            // This code will run when the "Create Wallet" button is clicked
            Log.d("WalletCreationActivity", "Create Wallet button clicked")

            // Show the EditText fields and TextView
            apiKeyEditText.visibility = android.view.View.VISIBLE
            userIdEditText.visibility = android.view.View.VISIBLE
            createUserTitle.visibility = android.view.View.VISIBLE
            submitButton.visibility =  android.view.View.VISIBLE
            backButton.visibility = android.view.View.VISIBLE

            // Hide the other component
            loginButton.visibility = android.view.View.INVISIBLE
            createWalletButton.visibility = android.view.View.INVISIBLE
        }

        backButton.setOnClickListener {
         // Show the EditText fields and TextView
            apiKeyEditText.visibility = android.view.View.INVISIBLE
            userIdEditText.visibility = android.view.View.INVISIBLE
            createUserTitle.visibility = android.view.View.INVISIBLE
            submitButton.visibility =  android.view.View.INVISIBLE
            backButton.visibility = android.view.View.INVISIBLE
            progressBar.visibility = android.view.View.INVISIBLE
            apiResponseTextView.visibility = android.view.View.INVISIBLE

            // Hide the other component
            loginButton.visibility = android.view.View.VISIBLE
            createWalletButton.visibility = android.view.View.VISIBLE

            // Clear the text in both userIdEditText and apiKeyEditText
            userIdEditText.text.clear()
            apiKeyEditText.text.clear()

            // Clear any existing error messages
            userIdEditText.error = null
            apiKeyEditText.error = null
        }

        submitButton.setOnClickListener {
            Log.d("WalletCreationActivity", "submit Wallet button clicked")
            // Get the text from the userIdEditText
            val userId = userIdEditText.text.toString().trim()
            if (userId.isEmpty()) {
                // userId is empty, display a warning message
                userIdEditText.error = "User ID is required"
            } else if (userId.length < 5 || userId.length > 50) {
                // userId length is not within the required range, display a warning message
                userIdEditText.error = "User ID must be between 5 and 50 characters"
            } else {
                // userId is valid, you can proceed with further actions here
                // Clear any previous error message
                userIdEditText.error = null
            }
            val apiKey = apiKeyEditText.text.toString().trim()
            if (apiKey.isEmpty()) {
                // userId is empty, display a warning message
                apiKeyEditText.error = "API Key is required"
            } else {
                // userId is valid, you can proceed with further actions here
                // Clear any previous error message
                apiKeyEditText.error = null
            }

            if (apiKeyEditText.error == null && userIdEditText.error == null) {
                // If there are no errors, proceed with the API call here
                progressBar.visibility = android.view.View.VISIBLE

                //call Circle API to create new user
                // Create OkHttp Client
                GlobalScope.launch(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaTypeOrNull()
                    val body = "{\"userId\":\"$userId\"}".toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://api.circle.com/v1/w3s/users")
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .addHeader("authorization", "Bearer $apiKey")
                        .build()

                    // Inside your try-catch block for making the API call
                    try {
                        val response = client.newCall(request).execute()

                        runOnUiThread {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                // Process the response data
                                Log.d("WalletCreationActivity", "Response: $responseBody")

                                //If the request is successful, you will receive an empty response body. -> {}
                                //redirect page with APIkey and userId -> Acquire a Session Token
                                // Create an Intent to start the AcquireSessionActivity
                                val intent = Intent(this@WalletCreationActivity, AcquireSessionTokenActivity::class.java)

                                // Pass the apiKey and userId as extras to the new activity
                                intent.putExtra("apiKey", apiKey)
                                intent.putExtra("userId", userId)

                                // Start the new activity
                                startActivity(intent)

                                // Finish the current activity if needed
                                finish()


                            } else {
                                // Handle error response
                                Log.e("WalletCreationActivity", "Error: ${response}")
                                progressBar.visibility = android.view.View.INVISIBLE
                                apiResponseTextView.visibility = android.view.View.VISIBLE
                                val errorCode = response.code // Assuming you have the error code from the API response
                                val errorMessage: String = when (errorCode) {
                                    401 -> "Invalid credentials"
                                    409 -> "Existing user already created with the provided userId."
                                    else -> "Unknown error"
                                }

                                apiResponseTextView.text = "Error ${errorCode}: ${errorMessage}. Please try again. "
                            }
                        }
                    } catch (e: IOException) {
                        // Handle network exception
                        Log.e("WalletCreationActivityError", "Error: ${e.message}", e)

                        runOnUiThread {
                            progressBar.visibility = android.view.View.INVISIBLE
                            apiResponseTextView.visibility = android.view.View.VISIBLE
                            apiResponseTextView.text = "Error: ${e.message}"
                        }
                    }

                }
            }
        }
        
    }
}
