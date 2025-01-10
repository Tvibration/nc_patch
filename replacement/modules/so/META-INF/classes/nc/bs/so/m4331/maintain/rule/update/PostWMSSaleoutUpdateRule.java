package nc.bs.so.m4331.maintain.rule.update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.so.m4331.entity.DeliveryBVO;
import nc.vo.so.m4331.entity.DeliveryHVO;
import nc.vo.so.m4331.entity.DeliveryVO;

/**
 * @description
 * <p>
 * <b>本类主要完成以下功能：</b>
 * <ul>
 * <li>发货单修改保存回写来源单据
 * @scene
 * 销售发货单修改保存后
 * @param
 * 无
 * @version 本版本号 6.0
 * @since 上一版本号 5.6
 * @author 祝会征
 * @time 2010-1-21 下午04:57:49
 */
public class PostWMSSaleoutUpdateRule {

  public void process(DeliveryVO[] vos) {
	  try {
			List<Map> mapList = new ArrayList();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			for (DeliveryVO ov : vos) {
					String pk_org_h = ov.getParentVO().getPk_org();
					//判断组织是否启用WMS同步
					String org_name = queryWMS001(pk_org_h);
					if(StringUtils.isEmpty(org_name)) {
						continue;
					}
					DeliveryHVO hvo = ov.getParentVO();
					DeliveryBVO[] bodyVos = ov.getChildrenVO();
					String srcSystemId = hvo.getCdeliveryid();//单据主键
					String vbillcode = hvo.getVbillcode();
					String customerid = bodyVos[0].getCordercustid();
					String billDate = hvo.getDbilldate().toString();
					String customerCode = (String)dao.executeQuery("select o.code from bd_customer o where o.pk_customer ='"+customerid+"'", new ColumnProcessor());
					String customerName = (String)dao.executeQuery("select o.name from bd_customer o where o.pk_customer ='"+customerid+"'", new ColumnProcessor());
					int delFlag = 0;
					String pk_org = hvo.getPk_org();
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String remark = hvo.getVnote();
					String billmakerID = hvo.getBillmaker();
					String makingPersonName = (String)dao.executeQuery("select o.user_name from sm_user o where o.cuserid='"+billmakerID+"'", new ColumnProcessor());
					String billmakerDate = hvo.getDmakedate().toString();
					String arrivalTime = hvo.getVdef1() == null? null : hvo.getVdef1() + " 00:00:00"; //物料到货时间
					String shipmentsTime = hvo.getDbilldate().toString();//发货时间
					
					//销售员
					String sql = "SELECT DISTINCT name FROM BD_PSNDOC bp \r\n" + 
							"JOIN so_saleorder ss ON bp.pk_psndoc = ss.cemployeeid\r\n" + 
							"JOIN so_delivery_b sb  ON sb.vsrccode = ss.VBILLCODE\r\n" + 
							"JOIN SO_DELIVERY sd ON sd.cdeliveryid = sb.cdeliveryid\r\n" + 
							"WHERE sd.VBILLCODE = '" + hvo.getVbillcode() + "' AND ss.dr=0 AND sb.dr = 0 AND  sd.dr = 0";
					String saleUser = (String)dao.executeQuery(sql,new ColumnProcessor());
					
					Map map = new HashMap();
					map.put("srcSystemId", srcSystemId);
					map.put("code", vbillcode);
					map.put("billDate", billDate);
					map.put("customerIdSrcSystemId", customerid);
					map.put("customerCode", customerCode);
					map.put("customerName", customerName);
					map.put("orgIdSrcSystemId", pk_org);
					map.put("delFlag", delFlag);
					map.put("orgCode", orgCode);
					map.put("orgName", orgName);
					map.put("remark", remark);
					map.put("makingPersonName", makingPersonName);
					map.put("makingBillDate", billmakerDate);
					map.put("arrivalTime",arrivalTime);
					map.put("shipmentsTime",shipmentsTime);
					map.put("saleUser",saleUser);
					
					List<Map> detailList = new ArrayList();
					
					DeliveryVO covo = new DeliveryVO();
					
					for(DeliveryBVO bodyVO : bodyVos) {
						Map bodymap = new HashMap();
						String bodyID = bodyVO.getCdeliverybid();
						String materialid = bodyVO.getCmaterialid();
						String frownote = bodyVO.getFrownote();
						String linoNo = bodyVO.getCrowno();
						String warehouseSrcSystemId = bodyVO.getCsendstordocid();
						String mainNum = bodyVO.getNnum().toString();
						String astNum = bodyVO.getNastnum().toString();
						String mainUnitSrcSystemId = bodyVO.getCunitid();
						String auxiliaryUnitSrcSystemId = bodyVO.getCastunitid();
						String vchangerate  = bodyVO.getVchangerate();
						String pk_custmaterial = bodyVO.getCcustmaterialid();//客户物料ID
						String custmaterialName = (String)dao.executeQuery("select o.code from bd_custmaterial o where o.pk_custmaterial='"+pk_custmaterial+"'", new ColumnProcessor());
						String cordercustid = bodyVO.getCordercustid();//客户ID
						String custcode = (String)dao.executeQuery("select o.code from bd_customer o where o.pk_customer ='"+cordercustid+"'", new ColumnProcessor());
						String vbatchcode = bodyVO.getVbatchcode();
						String conditions = bodyVO.getVbdef11(); //收货要求
						//物料名称
						String customerProductName = queryName("bd_custmaterial", "code", "pk_custmaterial", bodyVO.getCcustmaterialid());
						//型号
						String customerProductModel = queryName("bd_custmaterial", "name", "pk_custmaterial", bodyVO.getCcustmaterialid());
						//实发名称及型号
						String outProductNameAndModel = queryName("bd_material", "name", "pk_material", bodyVO.getCmaterialvid());
						//包装方式
						String packing = bodyVO.getVbdef15();
						//收货公司名称
						String receivingCustomerName = queryName("bd_customer", "name", "pk_customer", bodyVO.getCordercustid());
						//其它要求
						String otherRequirements = bodyVO.getFrownote();
						//收货联系人及联系方式
						String receiverNameAndReceiverPhone = bodyVO.getVbdef1() == null? "" : bodyVO.getVbdef1() + bodyVO.getVreceivetel() == null? "" :bodyVO.getVreceivetel() ;
						//计划或统计
						String planOrStatistics = bodyVO.getVbdef7();
						
						//收货地址
						String creceiveaddrid = bodyVO.getCreceiveaddrid();
						//国家
						String countryid = queryName("bd_address", "country", "pk_address", creceiveaddrid);
						String countryName = queryName("bd_countryzone", "name", "PK_COUNTRY", countryid);
						//省份
						String provinceid = queryName("bd_address", "PROVINCE", "pk_address", creceiveaddrid);
						String provinceName  = queryName("bd_region", "name", "pk_region", creceiveaddrid);
						//城市
						String cityid =  queryName("bd_address", "city", "pk_address", creceiveaddrid);
						String cityName  = queryName("bd_region", "name", "pk_region", cityid);
						//县区
						String vsectionid = queryName("bd_address", "vsection", "pk_address", creceiveaddrid);
						String vsectionName = queryName("bd_region", "name", "pk_region", vsectionid);
						//详细地址
						String detailInfo = queryName("bd_address", "DETAILINFO", "pk_address", creceiveaddrid);
						String receiverAddress = countryName == null? "":countryName + vsectionName == null? "" : vsectionName + detailInfo == null? "" : detailInfo;
						//运输要求
						String deliveryRequirements = bodyVO.getVbdef10();
						
						bodymap.put("srcSystemId", bodyID);
						bodymap.put("productSrcSystemId", materialid);
						bodymap.put("remark", frownote);
						bodymap.put("lineNo", linoNo);
		 				bodymap.put("warehouseSrcSystemId", warehouseSrcSystemId);
						bodymap.put("mainNum", mainNum);
						bodymap.put("astNum", astNum);
						bodymap.put("mainUnitSrcSystemId", mainUnitSrcSystemId);
						bodymap.put("auxiliaryUnitSrcSystemId", auxiliaryUnitSrcSystemId);
						bodymap.put("conversionRate", vchangerate);
						bodymap.put("delFlag", 0);
						bodymap.put("customerProductCode", custmaterialName);
						bodymap.put("customerCode", custcode);
						bodymap.put("batnoCodes", vbatchcode);
						bodymap.put("conditions",conditions);
						
						bodymap.put("customerProductName",customerProductName);
						bodymap.put("customerProductModel",customerProductModel);
						bodymap.put("outProductNameAndModel",outProductNameAndModel);
						bodymap.put("packing",packing);
						bodymap.put("receivingCustomerName",receivingCustomerName);
						bodymap.put("otherRequirements",otherRequirements);
						bodymap.put("deliveryRequirements",deliveryRequirements);
						bodymap.put("receiverNameAndReceiverPhone",receiverNameAndReceiverPhone);
						bodymap.put("planOrStatistics",planOrStatistics);
						bodymap.put("receiverAddress",receiverAddress);
						
						detailList.add(bodymap);
					}
					map.put("detailList", detailList);

					mapList.add(map);
			}
			if(mapList!=null&&mapList.size()>0) {
				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//				String jsons = js.toString().substring(1, js.toString().length()-1);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/stock/stock-sale-order/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, url, "so_delivery", "NC同步WMS销售发货");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, url, "so_delivery", "NC同步WMS销售发货");
					throw new BusinessException(e.toString());
				}
			}
	
	  } catch (Exception e) {
		  ExceptionUtils.wrappException(e);
	  }
  }
  
	private static String queryWMS001(String pk_org){
		String sql = "select name from bd_defdoc where pk_defdoclist=(select pk_defdoclist from bd_defdoclist where code='WMS001') and code=(select code from org_orgs where pk_org = '"+pk_org+"')";
		Object url;
		try {
			url = new BaseDAO().executeQuery(sql, new ColumnProcessor());
			if (null != url && !"".equals(url)) {
				return url.toString();
			} else {
				return null;
			}
		} catch (DAOException e) {
			return null;
		}
	}
	
	public static String queryName(String tablename, String nameORcode, String pk, String pkvalue) {
		if (StringUtils.isEmpty(pkvalue)) {
			return null;
		}
		String sql = "select " + nameORcode + " from " + tablename + " where " + pk + "='" + pkvalue
				+ "' and nvl(dr,0)=0";
		IUAPQueryBS bs = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		Object result;
		try {
			result = bs.executeQuery(sql, new ColumnProcessor());
			if (null != result && !"".equals(result)) {
				return result.toString();
			} else {
				return null;
			}
		} catch (BusinessException e) {
			return null;
		}
	}
}
