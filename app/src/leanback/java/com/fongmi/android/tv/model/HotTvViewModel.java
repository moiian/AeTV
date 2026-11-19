package com.fongmi.android.tv.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Vod;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HotTvViewModel extends ViewModel {

    private static final String URL = "https://movie.douban.com/j/search_subjects?type=tv&tag=%E7%83%AD%E9%97%A8&sort=recommend&page_limit=20&page_start=0";
    private static final long TIMEOUT_MS = 8_000;

    private final MutableLiveData<List<Vod>> items = new MutableLiveData<>();
    private final ViewModelTaskRunner<Request> tasks = new ViewModelTaskRunner<>(Request.class, App::post);

    private enum Request { HOT_TV }

    public LiveData<List<Vod>> getItems() {
        return items;
    }

    public void load() {
        if (items.getValue() != null) return;
        tasks.execute(Request.HOT_TV, TIMEOUT_MS, () -> parse(OkHttp.string(URL)),
                items::setValue, error -> items.setValue(Collections.emptyList()));
    }

    static List<Vod> parse(String response) {
        JsonElement subjects = Json.safeObject(Json.parse(response)).get("subjects");
        if (!(subjects instanceof JsonArray list)) return Collections.emptyList();
        List<Vod> items = new ArrayList<>();
        for (JsonElement element : list) {
            if (!element.isJsonObject()) continue;
            JsonObject subject = element.getAsJsonObject();
            String id = Json.safeString(subject, "id");
            String title = Json.safeString(subject, "title");
            if (id.isEmpty() || title.isEmpty()) continue;
            Vod item = new Vod();
            item.setId(id);
            item.setName(title);
            item.setPic(Json.safeString(subject, "cover"));
            items.add(item);
        }
        return items;
    }

    @Override
    protected void onCleared() {
        tasks.cancelAll();
    }
}
