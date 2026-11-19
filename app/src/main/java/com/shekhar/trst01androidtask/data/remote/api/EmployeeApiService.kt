package com.shekhar.trst01androidtask.data.remote.api

import com.shekhar.trst01androidtask.data.remote.model.EmployeeResponse
import retrofit2.Response
import retrofit2.http.GET

interface EmployeeApiService {
    @GET("v1/08f6af34-d99f-4ab2-bd09-6c9e953829d4")
    suspend fun getEmployees(): Response<EmployeeResponse>
}
