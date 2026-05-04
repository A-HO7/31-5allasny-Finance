package com.team31.financetracker.account.config.auth.handlers;

import com.team31.financetracker.account.config.auth.AuthContext;
import jakarta.servlet.ServletException;
import java.io.IOException;

public abstract class AuthHandler {

     private AuthHandler next;

     public AuthHandler setNext(AuthHandler next) {
          this.next = next;
          return next;
     }

     public boolean handle(AuthContext context) throws ServletException, IOException {
          if (process(context)) {
               if (next != null) {
                    return next.handle(context);
               }
               return true; // end of chain — all handlers passed
          }
          return false; // this handler failed — halt
     }

     protected abstract boolean process(AuthContext context) throws ServletException, IOException;
}
