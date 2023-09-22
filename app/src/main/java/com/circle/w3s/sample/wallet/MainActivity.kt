// Copyright (c) 2023, Circle Technologies, LLC. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.circle.w3s.sample.wallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.circle.w3s.sample.wallet.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrieve data from Intent
        val apiKey = intent.getStringExtra("apiKey")
        val userToken = intent.getStringExtra("userToken")
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val challengeId = intent.getStringExtra("challengeId")
        val appId = intent.getStringExtra("appId")

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(apiKey, userToken, encryptionKey, challengeId, appId))
                .commitNow()
        }
    }
}