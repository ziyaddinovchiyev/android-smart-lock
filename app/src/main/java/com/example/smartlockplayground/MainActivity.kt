package com.example.smartlockplayground

import android.content.IntentSender
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException


class MainActivity : AppCompatActivity() {

    private lateinit var userName: EditText
    private lateinit var password: EditText
    private lateinit var submit: Button
    private lateinit var set: Button
    private lateinit var remove: Button
    private lateinit var request: Button
    private lateinit var showHints: Button

    private lateinit var credential: Credential
    private lateinit var credentialsClient: CredentialsClient
    private lateinit var credentialRequest: CredentialRequest
    private lateinit var credentialsHintRequest: HintRequest

    private val resolveExceptionLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val credential = result.data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                credential?.let {
                    println(it.id)
                    println(it.password)
                    userName.setText(it.id)
                    password.setText(it.password)
                    setCredential()
                }
                Toast.makeText(this, "saved", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "save cancelled", Toast.LENGTH_LONG).show()
            }
        }

    private val hintLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val credential = result.data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                credential?.let {
                    println(it.id)
                    println(it.password)
                    userName.setText(it.id)
                    password.setText(it.password)
                    setCredential()
                }
                Toast.makeText(this, "retrieved", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "retrieve failed", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
        initComponents()

        showHints.setOnClickListener {
            launchSmartLockHint()
        }

        request.setOnClickListener {
            requestCredentials()
        }

        set.setOnClickListener {
            setCredential()
        }

        submit.setOnClickListener {
            saveCredentials()
        }

        remove.setOnClickListener {
            removeCredential()
        }
    }

    private fun removeCredential() {
        credentialsClient.delete(credential)
    }

    private fun saveCredentials() {
        setCredential()
        credentialsClient.save(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "saved", Toast.LENGTH_LONG).show()
                return@addOnCompleteListener
            }

            val exception = it.exception
            if (exception is ResolvableApiException) {
                try {
                    launchExceptionResolver(exception)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "couldn't resolve the request", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "save failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setCredential() {
        credential = Credential.Builder(userName.text.toString())
            .apply {
                if (!password.text.isNullOrBlank()) {
                    setPassword(password.text.toString())
                }
            }
            .build()
    }

    private fun requestCredentials() {
        credentialsClient.request(credentialRequest).addOnCompleteListener {
            if (it.isSuccessful) {
                userName.setText(it.result.credential?.id)
                password.setText(it.result.credential?.password)
                Toast.makeText(
                    this,
                    it.result.credential?.id + " : " + it.result.credential?.password,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val exception = it.exception
                if (exception is ResolvableApiException) {
                    Toast.makeText(this, "ResolvableApiException", Toast.LENGTH_LONG).show()
                    launchExceptionResolver(exception)
                } else if(exception is ApiException) {
                    Toast.makeText(this, "ApiException", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    private fun launchExceptionResolver(exception: ResolvableApiException) {
        resolveExceptionLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
    }

    private fun launchSmartLockHint() {
        hintLauncher.launch(
            IntentSenderRequest.Builder(
                credentialsClient.getHintPickerIntent(
                    credentialsHintRequest
                )
            ).build()
        )
    }

    private fun initComponents() {
        credentialsClient = Credentials.getClient(
            this,
            CredentialsOptions.Builder().forceEnableSaveDialog().build()
        )

        credentialsHintRequest = HintRequest.Builder()
            .setHintPickerConfig(
                CredentialPickerConfig.Builder()
                    .setShowCancelButton(true)
                    .build()
            )
            .setEmailAddressIdentifierSupported(true)
            .build()

        credentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()
    }

    private fun setupUI() {
        userName = findViewById(R.id.userName)
        password = findViewById(R.id.password)
        submit = findViewById(R.id.submit)
        set = findViewById(R.id.set)
        remove = findViewById(R.id.remove)
        request = findViewById(R.id.request)
        showHints = findViewById(R.id.showHints)
    }
}