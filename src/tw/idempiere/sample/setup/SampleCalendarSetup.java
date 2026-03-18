/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;

import org.compiere.model.MCalendar;
import org.compiere.model.MPeriod;
import org.compiere.model.MYear;
import org.compiere.util.CLogger;

/**
 * 會計年度和帳期建立
 * <p>
 * 負責建立會計年度（Calendar）、年度（Year）及 12 個帳期（Period）。
 * 使用當年度作為會計年度，並為每個月建立對應的帳期。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleCalendarSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleCalendarSetup.class);

    /** 會計年度名稱前綴 */
    private static final String CALENDAR_NAME = "天地人會計年度";

    /** 私有建構子，防止實例化 */
    private SampleCalendarSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立會計年度和帳期
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立 MCalendar（會計年度曆）</li>
     *   <li>建立 MYear（年度）</li>
     *   <li>建立 12 個 MPeriod（每月帳期）</li>
     *   <li>設定 Automatic Period Control = false</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID（通常為 0，表示 Client 層級）
     * @param trxName 交易名稱
     * @return 建立的 MCalendar，失敗時回傳 null
     */
    public static MCalendar createCalendar(Properties ctx, int clientId, int orgId, String trxName) {
        log.info("開始建立會計年度...");

        try {
            // 取得當前年度
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // 建立會計年度曆 (MCalendar)
            MCalendar calendar = new MCalendar(ctx, 0, trxName);
            calendar.setAD_Client_ID(clientId);
            calendar.setAD_Org_ID(orgId);
            calendar.setName(CALENDAR_NAME);
            calendar.setDescription("天地人實業有限公司會計年度曆");

            if (!calendar.save()) {
                log.severe("無法建立會計年度曆");
                return null;
            }
            log.info("已建立會計年度曆: " + calendar.getName());

            // 建立年度 (MYear)
            MYear year = createYear(ctx, calendar, currentYear, trxName);
            if (year == null) {
                log.severe("無法建立年度");
                return null;
            }

            // 建立 12 個帳期 (MPeriod)
            boolean periodsCreated = createPeriods(ctx, clientId, orgId, year, currentYear, trxName);
            if (!periodsCreated) {
                log.severe("無法建立帳期");
                return null;
            }

            log.info("會計年度建立完成，年度: " + currentYear);
            return calendar;

        } catch (Exception e) {
            log.severe("建立會計年度時發生錯誤: " + e.getMessage());
            return null;
        }
    }

    /**
     * 建立年度
     *
     * @param ctx 系統上下文
     * @param calendar 會計年度曆
     * @param yearValue 年度值
     * @param trxName 交易名稱
     * @return 建立的 MYear，失敗時回傳 null
     */
    private static MYear createYear(Properties ctx, MCalendar calendar, int yearValue, String trxName) {
        MYear year = new MYear(calendar);
        year.setFiscalYear(String.valueOf(yearValue));
        year.setDescription(yearValue + " 會計年度");

        if (!year.save()) {
            log.severe("無法儲存年度: " + yearValue);
            return null;
        }

        log.info("已建立年度: " + yearValue);
        return year;
    }

    /**
     * 建立 12 個月份帳期
     * <p>
     * 為指定年度建立 1-12 月的帳期，每個帳期的：
     * <ul>
     *   <li>起始日：該月第一天</li>
     *   <li>結束日：該月最後一天</li>
     *   <li>狀態：開啟（可輸入交易）</li>
     * </ul>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID
     * @param year 年度物件
     * @param yearValue 年度數值
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createPeriods(Properties ctx, int clientId, int orgId,
                                         MYear year, int yearValue, String trxName) {
        // 月份名稱
        String[] monthNames = {
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"
        };

        for (int month = 0; month < 12; month++) {
            // 計算每月的起始和結束日期
            Calendar startCal = Calendar.getInstance();
            startCal.set(yearValue, month, 1, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            Timestamp startDate = new Timestamp(startCal.getTimeInMillis());

            Calendar endCal = Calendar.getInstance();
            endCal.set(yearValue, month, 1, 23, 59, 59);
            endCal.set(Calendar.MILLISECOND, 999);
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Timestamp endDate = new Timestamp(endCal.getTimeInMillis());

            // 建立帳期
            MPeriod period = new MPeriod(year, month + 1, startDate, endDate);
            period.setAD_Client_ID(clientId);
            period.setAD_Org_ID(orgId);
            period.setName(yearValue + " " + monthNames[month]);
            period.setPeriodNo(month + 1);

            if (!period.save()) {
                log.severe("無法建立帳期: " + monthNames[month]);
                return false;
            }

            log.fine("已建立帳期: " + period.getName());
        }

        log.info("已建立 12 個月份帳期");
        return true;
    }
}
