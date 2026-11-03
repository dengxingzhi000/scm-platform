package com.scmcloud.inventory.api;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 搴撳瓨 TCC 鏈嶅姟鎺ュ彛
 *
 * <p>TCC (Try-Confirm-Cancel) 妯″紡瀹炵幇搴撳瓨棰勭暀
 *
 * <p>涓夐樁娈佃鏄庯細
 * <ul>
 *   <li>Try: 棰勭暀搴撳瓨锛堝皢鍙敤搴撳瓨杞负閿佸畾搴撳瓨锟?li>
 *   <li>Confirm: 纭鎵e噺锛堟墸鍑忛攣瀹氬簱瀛橈級</li>
 *   <li>Cancel: 鍙栨秷棰勭暀锛堥噴鏀鹃攣瀹氬簱瀛樹负鍙敤搴撳瓨锟?li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@LocalTCC
public interface InventoryTccService {

    /**
     * Try 闃舵锛氶鐣欏簱锟?
     *
     * <p>灏嗗彲鐢ㄥ簱瀛樿浆涓洪攣瀹氬簱瀛橈紝浣嗕笉瀹為檯鎵e噺
     *
     * @param skuId SKU ID
     * @param quantity 棰勭暀鏁伴噺
     * @param businessKey 涓氬姟閿紙璁㈠崟鍙凤級锛岀敤浜庡箓绛夋€ф帶锟?
     * @return 棰勭暀鏄惁鎴愬姛
     */
    @TwoPhaseBusinessAction(
            name = "reserveInventory",
            commitMethod = "confirmReserve",
            rollbackMethod = "cancelReserve"
    )
    boolean reserveInventory(
            @BusinessActionContextParameter(paramName = "skuId") Long skuId,
            @BusinessActionContextParameter(paramName = "quantity") Integer quantity,
            @BusinessActionContextParameter(paramName = "businessKey") String businessKey
    );

    /**
     * Confirm 闃舵锛氱'璁ら锟?
     *
     * <p>鎵e噺閿佸畾搴撳瓨锛屽畬鎴愭渶缁堟墸锟?
     *
     * @param context TCC 涓婁笅锟?
     * @return 纭鏄惁鎴愬姛
     */
    boolean confirmReserve(BusinessActionContext context);

    /**
     * Cancel 闃舵锛氬彇娑堥锟?
     *
     * <p>閲婃斁閿佸畾搴撳瓨锛屾仮澶嶄负鍙敤搴撳瓨
     *
     * @param context TCC 涓婁笅锟?
     * @return 鍙栨秷鏄惁鎴愬姛
     */
    boolean cancelReserve(BusinessActionContext context);
}