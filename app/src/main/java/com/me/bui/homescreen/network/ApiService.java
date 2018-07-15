package com.me.bui.homescreen.network;

import com.me.bui.homescreen.network.model.Country;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by mao.bui on 7/15/2018.
 */
public interface ApiService {
    @GET("rest/v2/name/{name}")
    Single<List<Country>> getCountryByName(@Path("name") String name);
}
