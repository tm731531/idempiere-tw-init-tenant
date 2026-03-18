/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.cleanup;

import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Trx;

/**
 * 天地人實業示範 Client 清理
 * <p>
 * 負責在插件卸載時清理所有示範資料。
 * 依照資料表的相依性順序刪除，確保不會有 FK 違反。
 * </p>
 * <p>
 * 刪除順序（由葉子到根）：
 * <ol>
 *   <li>交易資料（Invoice, Order, InOut 等）</li>
 *   <li>BOM</li>
 *   <li>商品價格、商品</li>
 *   <li>業務夥伴</li>
 *   <li>價格表</li>
 *   <li>稅務設定</li>
 *   <li>會計架構</li>
 *   <li>會計年度</li>
 *   <li>倉庫</li>
 *   <li>組織</li>
 *   <li>角色、用戶</li>
 *   <li>Client</li>
 * </ol>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleClientCleanup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleClientCleanup.class);

    /** 私有建構子，防止實例化 */
    private SampleClientCleanup() {
        // 工具類別，不需要實例化
    }

    /** Client 名稱（MSetup 會把 Name 同時設定給 Value） */
    private static final String CLIENT_NAME = "天地人實業有限公司";

    /**
     * 執行清理
     * <p>
     * 檢查 Sample Client 是否存在，若存在則依序刪除所有相關資料。
     * 整個清理作業在一個 Transaction 中執行，確保 All or Nothing。
     * </p>
     * <p>
     * 注意：使用直接 SQL 查詢而非 Query API，避免在 OSGi stop() 階段
     * Env 環境不完整的問題。
     * </p>
     */
    public static void cleanup() {
        System.out.println("=== SampleClientCleanup.cleanup() 開始執行 ===");
        log.info("SampleClientCleanup.cleanup() 開始執行...");

        int clientId = -1;
        try {
            // 使用直接 SQL 查詢（MSetup 把 Name 同時設定給 Value）
            clientId = DB.getSQLValue(null,
                "SELECT AD_Client_ID FROM AD_Client WHERE Value = ?", CLIENT_NAME);
            System.out.println("=== 查詢結果: clientId=" + clientId + " ===");
        } catch (Exception e) {
            System.out.println("=== 查詢 Client 時發生錯誤: " + e.getMessage() + " ===");
            e.printStackTrace();
        }

        log.info("查詢 Client Value='" + CLIENT_NAME + "' 結果: clientId=" + clientId);

        if (clientId <= 0) {
            System.out.println("=== 天地人實業 Client 不存在 (clientId=" + clientId + ")，無需清理 ===");
            log.info("天地人實業 Client 不存在，無需清理");
            return;
        }

        System.out.println("=== 開始清理 Client ID=" + clientId + " ===");
        log.info("開始清理天地人實業 Client (ID=" + clientId + ")...");

        String trxName = Trx.createTrxName("SampleCleanup");
        Trx trx = Trx.get(trxName, true);

        try {
            // 依相依性順序刪除
            // 1. 刪除交易資料（若有）
            deleteTransactions(clientId, trxName);

            // 2. 刪除 BOM
            deleteBOMs(clientId, trxName);

            // 3. 刪除 Product Price
            deleteProductPrices(clientId, trxName);

            // 4. 刪除 Product
            deleteProducts(clientId, trxName);

            // 5. 刪除 BP
            deleteBPartners(clientId, trxName);

            // 6. 刪除 BP Group
            deleteBPGroups(clientId, trxName);

            // 7. 刪除 Product Category
            deleteProductCategories(clientId, trxName);

            // 8. 刪除 Price List
            deletePriceLists(clientId, trxName);

            // 9. 刪除 Tax
            deleteTax(clientId, trxName);

            // 10. 刪除 Accounting Schema
            deleteAccountingSchema(clientId, trxName);

            // 11. 刪除 Calendar
            deleteCalendar(clientId, trxName);

            // 12. 刪除 Warehouse, Locator
            deleteWarehouses(clientId, trxName);

            // 13. 刪除 Organization
            deleteOrganizations(clientId, trxName);

            // 14. 刪除 User, Role
            deleteUsersAndRoles(clientId, trxName);

            // 15. 刪除 ClientInfo
            deleteClientInfo(clientId, trxName);

            // 16. 刪除 Client
            deleteClient(clientId, trxName);

            trx.commit();
            log.info("天地人實業 Client 清理完成");

        } catch (Exception e) {
            log.log(Level.SEVERE, "清理失敗", e);
            trx.rollback();
        } finally {
            trx.close();
        }
    }

    /**
     * 刪除交易資料
     * <p>
     * 刪除可能存在的各種交易單據：
     * 發票、出入庫、訂單、移動、盤點、付款、銀行對帳、日記帳等
     * </p>
     */
    private static void deleteTransactions(int clientId, String trxName) {
        String[] tables = {
            "C_InvoiceLine", "C_Invoice",
            "M_InOutLine", "M_InOut",
            "C_OrderLine", "C_Order",
            "M_MovementLine", "M_Movement",
            "M_InventoryLine", "M_Inventory",
            "C_Payment", "C_BankStatementLine", "C_BankStatement",
            "Fact_Acct", "GL_JournalLine", "GL_Journal", "GL_JournalBatch"
        };

        for (String table : tables) {
            deleteFromTable(table, clientId, trxName);
        }
    }

    /**
     * 刪除 BOM 資料
     */
    private static void deleteBOMs(int clientId, String trxName) {
        deleteFromTable("PP_Product_BOMLine", clientId, trxName);
        deleteFromTable("PP_Product_BOM", clientId, trxName);
    }

    /**
     * 刪除商品價格
     * <p>
     * 使用子查詢刪除，因為 M_ProductPrice 的 FK 是透過 M_PriceList_Version。
     * </p>
     */
    private static void deleteProductPrices(int clientId, String trxName) {
        DB.executeUpdate("DELETE FROM M_ProductPrice WHERE M_PriceList_Version_ID IN " +
            "(SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE M_PriceList_ID IN " +
            "(SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID = ?))",
            new Object[]{clientId}, false, trxName);
    }

    /**
     * 刪除商品
     */
    private static void deleteProducts(int clientId, String trxName) {
        // 先刪除 Product Acct
        deleteFromTable("M_Product_Acct", clientId, trxName);
        deleteFromTable("M_Product", clientId, trxName);
    }

    /**
     * 刪除業務夥伴
     * <p>
     * 包含 BP 的銀行帳戶、地址、聯絡人等子資料。
     * 注意：AD_User 中的 BP Contact 會在此處刪除。
     * </p>
     */
    private static void deleteBPartners(int clientId, String trxName) {
        deleteFromTable("C_BP_BankAccount", clientId, trxName);
        deleteFromTable("C_BPartner_Location", clientId, trxName);
        // 注意：AD_User 包含 BP 的聯絡人，但也包含系統用戶
        // 這裡只刪除有關聯 BP 的 User，系統用戶會在 deleteUsersAndRoles 中處理
        DB.executeUpdate("DELETE FROM AD_User WHERE AD_Client_ID = ? AND C_BPartner_ID IS NOT NULL",
            new Object[]{clientId}, false, trxName);
        deleteFromTable("C_BPartner", clientId, trxName);
    }

    /**
     * 刪除 BP 群組
     */
    private static void deleteBPGroups(int clientId, String trxName) {
        deleteFromTable("C_BP_Group_Acct", clientId, trxName);
        deleteFromTable("C_BP_Group", clientId, trxName);
    }

    /**
     * 刪除商品類別
     */
    private static void deleteProductCategories(int clientId, String trxName) {
        deleteFromTable("M_Product_Category_Acct", clientId, trxName);
        deleteFromTable("M_Product_Category", clientId, trxName);
    }

    /**
     * 刪除價格表
     */
    private static void deletePriceLists(int clientId, String trxName) {
        deleteFromTable("M_PriceList_Version", clientId, trxName);
        deleteFromTable("M_PriceList", clientId, trxName);
    }

    /**
     * 刪除稅務設定
     */
    private static void deleteTax(int clientId, String trxName) {
        deleteFromTable("C_Tax_Acct", clientId, trxName);
        deleteFromTable("C_Tax", clientId, trxName);
        deleteFromTable("C_TaxCategory", clientId, trxName);
    }

    /**
     * 刪除會計架構
     * <p>
     * 包含預設科目設定、科目元素、GL 設定、有效組合、科目等。
     * </p>
     */
    private static void deleteAccountingSchema(int clientId, String trxName) {
        deleteFromTable("C_AcctSchema_Default", clientId, trxName);
        deleteFromTable("C_AcctSchema_Element", clientId, trxName);
        deleteFromTable("C_AcctSchema_GL", clientId, trxName);
        deleteFromTable("C_ValidCombination", clientId, trxName);
        deleteFromTable("C_AcctSchema", clientId, trxName);
        deleteFromTable("C_ElementValue", clientId, trxName);
        deleteFromTable("C_Element", clientId, trxName);
    }

    /**
     * 刪除會計年度
     */
    private static void deleteCalendar(int clientId, String trxName) {
        deleteFromTable("C_PeriodControl", clientId, trxName);
        deleteFromTable("C_Period", clientId, trxName);
        deleteFromTable("C_Year", clientId, trxName);
        deleteFromTable("C_Calendar", clientId, trxName);
    }

    /**
     * 刪除倉庫和儲位
     */
    private static void deleteWarehouses(int clientId, String trxName) {
        deleteFromTable("M_Locator", clientId, trxName);
        deleteFromTable("M_Warehouse_Acct", clientId, trxName);
        deleteFromTable("M_Warehouse", clientId, trxName);
    }

    /**
     * 刪除組織
     * <p>
     * 注意：不刪除 AD_Org_ID = 0 的 * 組織（系統組織）
     * </p>
     */
    private static void deleteOrganizations(int clientId, String trxName) {
        // 先刪除非 * 組織的 OrgInfo
        DB.executeUpdate("DELETE FROM AD_OrgInfo WHERE AD_Org_ID IN " +
            "(SELECT AD_Org_ID FROM AD_Org WHERE AD_Client_ID = ? AND AD_Org_ID > 0)",
            new Object[]{clientId}, false, trxName);
        // 刪除非 * 組織
        DB.executeUpdate("DELETE FROM AD_Org WHERE AD_Client_ID = ? AND AD_Org_ID > 0",
            new Object[]{clientId}, false, trxName);
    }

    /**
     * 刪除用戶和角色
     */
    private static void deleteUsersAndRoles(int clientId, String trxName) {
        deleteFromTable("AD_User_Roles", clientId, trxName);
        deleteFromTable("AD_Role_OrgAccess", clientId, trxName);
        deleteFromTable("AD_Window_Access", clientId, trxName);
        deleteFromTable("AD_Process_Access", clientId, trxName);
        deleteFromTable("AD_Form_Access", clientId, trxName);
        deleteFromTable("AD_Task_Access", clientId, trxName);
        deleteFromTable("AD_Workflow_Access", clientId, trxName);
        deleteFromTable("AD_Role", clientId, trxName);
        // 刪除剩餘的用戶（已在 deleteBPartners 中刪除 BP 關聯的用戶）
        deleteFromTable("AD_User", clientId, trxName);
    }

    /**
     * 刪除 ClientInfo
     */
    private static void deleteClientInfo(int clientId, String trxName) {
        deleteFromTable("AD_ClientInfo", clientId, trxName);
    }

    /**
     * 刪除 Client
     */
    private static void deleteClient(int clientId, String trxName) {
        DB.executeUpdate("DELETE FROM AD_Client WHERE AD_Client_ID = ?",
            new Object[]{clientId}, false, trxName);
        log.info("Client 刪除完成: " + clientId);
    }

    /**
     * 通用刪除方法
     * <p>
     * 根據 AD_Client_ID 刪除指定資料表中的所有記錄。
     * 發生錯誤時僅記錄警告，不中斷清理流程。
     * </p>
     *
     * @param tableName 資料表名稱
     * @param clientId Client ID
     * @param trxName 交易名稱
     */
    private static void deleteFromTable(String tableName, int clientId, String trxName) {
        try {
            int count = DB.executeUpdate(
                "DELETE FROM " + tableName + " WHERE AD_Client_ID = ?",
                new Object[]{clientId}, false, trxName);
            if (count > 0) {
                log.fine("已從 " + tableName + " 刪除 " + count + " 筆");
            }
        } catch (Exception e) {
            log.warning("刪除 " + tableName + " 時發生錯誤: " + e.getMessage());
        }
    }
}
