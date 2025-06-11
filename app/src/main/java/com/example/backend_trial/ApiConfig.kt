 package com.example.backend_trial

object ApiConfig {
    // Base URL for the API
    const val BASE_URL = "http://192.168.0.111:8000"  // Update this with your actual deployed API URL
    //const val BASE_URL = "http://157.180.120.45:8000"

    // API endpoints
    const val ANALYZE_IRIS_ENDPOINT = "/analyze_iris/"
    
    // Timeouts in seconds
    const val CONNECT_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    const val READ_TIMEOUT = 60L
}