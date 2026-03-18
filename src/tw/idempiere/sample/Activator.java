package tw.idempiere.sample;

import java.util.logging.Level;
import org.compiere.util.CLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * 天地人實業示範資料插件 - OSGi Activator
 *
 * 生命週期：
 * - start(): 檢查並建立示範 Client
 * - stop(): 保留資料（不做任何事）
 * - uninstall: 透過 BundleListener 清理資料
 */
public class Activator implements BundleActivator {

    private static final CLogger log = CLogger.getCLogger(Activator.class);
    private static final String CLIENT_VALUE = "sample";
    private BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        log.info("天地人實業示範資料插件啟動中...");

        // TODO: 實作 SampleClientSetup.init()
        log.info("天地人實業示範資料插件啟動完成");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("天地人實業示範資料插件停止（資料保留）");
        // 不做任何事，保留資料
    }
}
