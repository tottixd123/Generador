package com.example.gemerador.Trabajador;

import com.example.gemerador.Data_base.Ticket;
import java.util.List;

public class TrabajadorServiceCallbacks {
    public interface OnTicketsLoadedListener {
        void onTicketsLoaded(List<Ticket> tickets);
        void onError(String error);
    }

    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(String error);
    }
}