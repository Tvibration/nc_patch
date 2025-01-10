package nc.bs.pub.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.pub.compiler.AbstractCompiler2;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.ic.m4k.IWhsTransApprove;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.ic.m4k.entity.WhsTransBillVO;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.compiler.PfParameterVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pub.power.PowerActionEnum;
import nc.vo.scmpub.res.billtype.ICBillType;
import nccloud.vo.scmpub.utils.power.DataPermissionUtil;

public class N_4K_APPROVE extends AbstractCompiler2
{
  private Hashtable m_methodReturnHas = new Hashtable();
  private Hashtable m_keyHas = null;

  public Object runComClass(PfParameterVO vo)
    throws BusinessException
  {
    try
    {
      this.m_tmpVo = vo;
      Object retValue = null;

      WhsTransBillVO[] vos = (WhsTransBillVO[])(WhsTransBillVO[])getVos();

      if (vo.isCloudEntry) {
        DataPermissionUtil.checkPermission(vos, ICBillType.TransferWarehouse.getCode(), PowerActionEnum.APPROVE.getActioncode(), "vbillcode");
      }

      retValue = ((IWhsTransApprove)NCLocator.getInstance().lookup
        (IWhsTransApprove.class))
        .approve(vos, this);
//      for(int i=0;i<vos.length;i++) {
//    	  WhsTransBillBodyVO[] bodys = vos[i].getBodys();
//    	  String csourcetype = bodys[0].getCsourcetype();
//    	  if(csourcetype!=null&&csourcetype.equals("55AC"))
//    		  sendWMS(vos[i]);
//    	  else
//    		  sendWMS2(vos[i]);
//      }
      
      return retValue;
    }
    catch (Exception ex) {
      ExceptionUtils.marsh(ex);
    }
    return null;
  }
  
  /**
   * 转库单传WMS领料单（备料计划相关）
   * zhoush
   * 20240228
   */
  private void sendWMS(WhsTransBillVO cv) throws BusinessException{
	  BaseDAO dao = new BaseDAO();
	  List<Map> mapList = new ArrayList();
		  WhsTransBillHeaderVO phv = cv.getHead();
		  WhsTransBillBodyVO[] pbv = cv.getBodys();
		  String def1 = phv.getVdef1();
		  if(def1!=null&&def1.equals("Y"))  //WMS同步过来的就不再同步回WMS，避免循环调用
			  return;
		  String pk_org = phv.getPk_org();
		  String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
		  if(mesFlag==null||mesFlag.equals("N"))
				return;
		  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
		  String billtypecode = (String)dao.executeQuery("select t.pk_billtypecode from bd_billtype t where t.pk_billtypeid='"+phv.getCtrantypeid()+"'", new ColumnProcessor());
		  
		  Map map = new HashMap();
//		  String departmentName = (String)dao.executeQuery("select d.name from org_dept d where d.pk_dept='"+phv.getCdeptid()+"'", new ColumnProcessor());
//		  String customerCode = (String)dao.executeQuery("select c.code from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
//		  String customerName = (String)dao.executeQuery("select c.name from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
		  map.put("srcSystemId",phv.getCspecialhid());
		  map.put("code",phv.getVbillcode());
		  map.put("billDate",phv.getDmakedate().toString());
		  map.put("delFlag",0);
		  map.put("orgIdSrcSystemId",pk_org);
		  map.put("orgCode",orgCode);
		  map.put("orgName",orgName);
		  map.put("type",billtypecode);
		  map.put("remark",phv.getVnote());
		  map.put("makingPersonName",Billmaker);
		  map.put("makingBillDate", phv.getDmakedate().toString());
		  map.put("receiveWorkshopWarehouseSrcSystemId",phv.getCotherwhid());
		  map.put("distributionMaterialWarehouseSrcSystemId",phv.getCwarehouseid());

		  List<Map> mapList2 = new ArrayList();
		  for(int j=0;j<pbv.length;j++) {
			  Map map_b = new HashMap();
			  map_b.put("srcSystemId", pbv[j].getCspecialbid());
			  map_b.put("lineNo", pbv[j].getCrowno());
			  map_b.put("remark", pbv[j].getVnotebody());
			  map_b.put("productSrcSystemId", pbv[j].getCmaterialoid());
			  map_b.put("mainUnitSrcSystemId", pbv[j].getCunitid());
			  map_b.put("auxiliaryUnitSrcSystemId", pbv[j].getCastunitid());
			  map_b.put("shouldPickingNum", pbv[j].getNassistnum().toString());
			  map_b.put("shouldPickingMainNum", pbv[j].getNnum().toString());
			  map_b.put("conversionRate", pbv[j].getVchangerate());
			  map_b.put("batchCode", pbv[j].getVbatchcode());
//			  map_b.put("isExcessQuantityScan", 0);
			  map_b.put("delFlag", 0);
			  mapList2.add(map_b);
		  }
		  map.put("allocationOrderItemList", mapList2);
		  mapList.add(map);
	  
	  if(mapList!=null&&mapList.size()>0) {
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/production/picking-order/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, url, "ic_whstrans_h", "NC转库单同步WMS");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, url, "ic_whstrans_h", "NC转库单同步WMS");
				throw new BusinessException(e.toString());
			}
		}
  }
  
  /**
         * 转库单传WMS转库单（仓库间转库） 
   * zhoush
   * 20240228
   */
  private void sendWMS2(WhsTransBillVO cv) throws BusinessException{
	  BaseDAO dao = new BaseDAO();
	  List<Map> mapList = new ArrayList();
		  WhsTransBillHeaderVO phv = cv.getHead();
		  WhsTransBillBodyVO[] pbv = cv.getBodys();
		  String pk_org = phv.getPk_org();
		  String def1 = phv.getVdef1();
		  if(def1==null||def1.equals("Y"))  //WMS同步过来的就不再同步回WMS，避免循环调用
			  return;
		  String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
		  if(mesFlag!=null&&mesFlag.equals("N"))
				return;
		  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
		  String billtypecode = (String)dao.executeQuery("select t.pk_billtypecode from bd_billtype t where t.pk_billtypeid='"+phv.getCtrantypeid()+"'", new ColumnProcessor());
		  Map map = new HashMap();
//		  String departmentName = (String)dao.executeQuery("select d.name from org_dept d where d.pk_dept='"+phv.getCdeptid()+"'", new ColumnProcessor());
//		  String customerCode = (String)dao.executeQuery("select c.code from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
//		  String customerName = (String)dao.executeQuery("select c.name from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
		  map.put("srcSystemId",phv.getCspecialhid());
		  map.put("code",phv.getVbillcode());
		  map.put("billDate",phv.getDmakedate().toString());
		  map.put("delFlag",0);
		  map.put("orgIdSrcSystemId",pk_org);
		  map.put("orgCode",orgCode);
		  map.put("orgName",orgName);
		  map.put("type",billtypecode);
		  map.put("remark",phv.getVnote());
		  map.put("makingPersonName",Billmaker);
		  map.put("makingBillDate", phv.getDmakedate().toString());
		  map.put("targetWarehouseSrcSystemId",phv.getCotherwhid());
		  map.put("sourceWarehouseSrcSystemId",phv.getCwarehouseid());

		  List<Map> mapList2 = new ArrayList();
		  for(int j=0;j<pbv.length;j++) {
			  Map map_b = new HashMap();
			  map_b.put("srcSystemId", pbv[j].getCspecialbid());
			  map_b.put("lineNo", pbv[j].getCrowno());
			  map_b.put("remark", pbv[j].getVnotebody());
			  map_b.put("productSrcSystemId", pbv[j].getCmaterialoid());
			  map_b.put("mainUnitSrcSystemId", pbv[j].getCunitid());
			  map_b.put("auxiliaryUnitSrcSystemId", pbv[j].getCastunitid());
			  map_b.put("shouldNum", pbv[j].getNassistnum().toString());
			  map_b.put("shouldMainNum", pbv[j].getNnum().toString());
			  map_b.put("conversionRate", pbv[j].getVchangerate());
			  map_b.put("batchCode", pbv[j].getVbatchcode());
//			  map_b.put("isExcessQuantityScan", 0);
			  map_b.put("delFlag", 0);
			  mapList2.add(map_b);
		  }
		  map.put("allocationOrderItemList", mapList2);
		  mapList.add(map);
	  
	  if(mapList!=null&&mapList.size()>0) {
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/stock/allocation-order/deptReceives/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "ic_whstrans_h", "NC转库单同步WMS");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "ic_whstrans_h", "NC转库单同步WMS");
				throw new BusinessException(e.toString());
			}
		}
  }

  public String getCodeRemark()
  {
    return NCLangRes4VoTransl.getNCLangRes().getStrByID("4008010_0", "04008010-0007");
  }

  private void setParameter(String key, Object val)
  {
    if (this.m_keyHas == null)
      this.m_keyHas = new Hashtable();

    if (val != null)
      this.m_keyHas.put(key, val);
  }
}