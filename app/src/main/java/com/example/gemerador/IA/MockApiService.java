package com.example.gemerador.IA;

import com.example.gemerador.Data_base.Ticket;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import java.util.List;

public interface MockApiService {
    @GET("tickets")
    Call<List<Ticket>> getTickets();

    @GET("tickets/{id}")
    Call<Ticket> getTicket(@Path("id") String id);

    @POST("tickets")
    Call<Ticket> createTicket(@Body Ticket ticket);

    @PUT("tickets/{id}")
    Call<Ticket> updateTicket(@Path("id") String id, @Body Ticket ticket);
}