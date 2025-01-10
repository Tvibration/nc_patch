package nc.bs.pub.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.pub.compiler.AbstractCompiler2;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.to.m5x.ITransOrderMaintain;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.compiler.PfParameterVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.to.m5x.entity.BillHeaderVO;
import nc.vo.to.m5x.entity.BillItemVO;
import nc.vo.to.m5x.entity.BillVO;

public class N_5X_APPROVE extends AbstractCompiler2
{
  public String getCodeRemark()
  {
    return "\n";
  }

  public Object runComClass(PfParameterVO vo)
    throws BusinessException
  {
    this.m_tmpVo = vo;
    BillVO[] bills = null;

    ITransOrderMaintain maintain = (ITransOrderMaintain)NCLocator.getInstance().lookup(ITransOrderMaintain.class);
    try {
      bills = maintain.approveTransOrder(this);
    }
    catch (Exception e)
    {
      ExceptionUtils.marsh(e);
    }
    sendWMS(bills);
    return bills;
  }
  
  /**
   * 调拨订单传WMS
   * zhoush
   * 20240309
   */
  private void sendWMS(BillVO[] BillVOs) throws BusinessException{
	  BaseDAO dao = new BaseDAO();
	  List<Map> mapList = new ArrayList();
	  for(int i=0;i<BillVOs.length;i++) {
		  
		  BillVO cv = BillVOs[i];
		  BillHeaderVO phv = cv.getParentVO();
		  BillItemVO[] pbv = (BillItemVO[])cv.getChildrenVO();
		  String pk_org = phv.getPk_org();
		  String def20 = phv.getVdef20();
		  if(def20!=null&&def20.equals("Y"))  //不推送WMS勾选的话不推
			  continue;
		  String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
		  if(mesFlag==null||mesFlag.equals("N"))
				continue;
		  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
		  
		  Map map = new HashMap();
		  String wlcode = (String)dao.executeQuery("select m.code from bd_material m where m.pk_material='"+pbv[0].getCinventoryid()+"'", new ColumnProcessor());
		  
		  map.put("outOrgSrcSystemId",phv.getPk_org());
		  map.put("inOrgSrcSystemId",phv.getCinstockorgid());
		  map.put("outWarehouseSrcSystemId",pbv[0].getCoutstordocid());
		  map.put("inWarehouseSrcSystemId",pbv[0].getCinstordocid());
		  map.put("billDate",phv.getDbilldate().toString());
		  map.put("typeCode",wlcode.startsWith("03")?"2":"1");
		  
		  map.put("remark",phv.getVnote());
		  map.put("srcSystem","NC");
		  map.put("srcSystemId",phv.getCbillid());
		  map.put("srcSystemCode", phv.getVbillcode());
		  map.put("srcSystemName","");
		  List<Map> mapList2 = new ArrayList();
		  for(int j=0;j<pbv.length;j++) {
			  Map map_b = new HashMap();
			  map_b.put("outWarehouseSrcSystemId", pbv[j].getCoutstordocid());
			  map_b.put("lineNo", pbv[j].getCrowno());
			  map_b.put("productSrcSystemId", pbv[j].getCinventoryid());
			  map_b.put("batchCode", pbv[j].getVbatchcode());
			  map_b.put("mainUnitSrcSystemId", pbv[j].getCunitid());
			  map_b.put("auxiliaryUnitSrcSystemId", pbv[j].getCastunitid());
			  map_b.put("conversionRate", pbv[j].getVchangerate());
			  map_b.put("shouldMainNum", pbv[j].getNnum().toString());
			  map_b.put("shouldNum", pbv[j].getNastnum().toString());
			  map_b.put("remark", pbv[j].getVbnote());
			  map_b.put("srcSystem", "NC");
			  map_b.put("srcSystemId", pbv[j].getCbill_bid());
			  map_b.put("srcSystemCode", "");
			  map_b.put("srcSystemName", "");
			  mapList2.add(map_b);
		  }
		  map.put("transferOrderDetailList", mapList2);
		  mapList.add(map);
	  }
	  if(mapList!=null&&mapList.size()>0) {
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//			String jsons = js.toString().substring(1, js.toString().length()-1);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/purchase/transfer-order/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "to_bill", "调拨订单同步WMS");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "to_bill", "调拨订单同步WMS");
				throw new BusinessException(e.toString());
			}
		}
  }
}