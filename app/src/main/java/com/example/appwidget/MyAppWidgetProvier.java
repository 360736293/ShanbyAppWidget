package com.example.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

public class MyAppWidgetProvier extends AppWidgetProvider {

    String checkinDate;
    String checkinDaysNum;
    String checkinStatus;
    String usedTime;
    String num;
    Map<String, String> statusMap = new HashMap<>();
    int flag = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        startThread();
        while (flag == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        String text =
                        "天数：" + checkinDaysNum + "\n" +
                        "数量：" + num + "\n" +
                        "时长：" + (int) (Double.parseDouble(usedTime) / 60) + "min" + "\n\n" +
                        "状态：" + statusMap.getOrDefault(checkinStatus, "未打卡");
        SpannableString spannableString = new SpannableString(text);
        ForegroundColorSpan foregroundColorSpan;
        if (ObjectUtil.equal(checkinStatus, "HAVE_CHECKIN")) {
            foregroundColorSpan = new ForegroundColorSpan(Color.GREEN);
        } else {
            foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        }
        int index = text.indexOf("已打卡") > 0 ? text.indexOf("已打卡") : text.indexOf("未打卡");
        spannableString.setSpan(foregroundColorSpan, index, index + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetprovider_layout);
        remoteViews.setTextViewText(R.id.tv_text, spannableString);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    public void startThread() {
        Thread thread = new Thread(() -> {
            getShanbyText();
            flag = 1;
        });
        thread.start();
    }

    /**
     * 获取扇贝打卡相关内容
     */
    public void getShanbyText() {
        statusMap.put("HAVE_CHECKIN", "已打卡");
        HttpRequest get = HttpUtil.createGet("https://apiv3.shanbay.com/uc/checkin");
        Map<String, String> headers = new HashMap<>();
        headers.put("cookie", "");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        get.addHeaders(headers);
        HttpResponse execute = get.execute();
        String text = UnicodeUtil.toString(execute.body());
        JSONObject data = JSONUtil.parseObj(text);
        checkinDate = data.get("date").toString();
        checkinDaysNum = data.get("checkin_days_num").toString();
        checkinStatus = data.get("status").toString();
        usedTime = JSONUtil.parseObj(JSONUtil.parseArray(data.get("tasks")).get(0)).get("used_time").toString();
        num = JSONUtil.parseObj(JSONUtil.parseArray(data.get("tasks")).get(0)).get("num").toString();
    }
}
