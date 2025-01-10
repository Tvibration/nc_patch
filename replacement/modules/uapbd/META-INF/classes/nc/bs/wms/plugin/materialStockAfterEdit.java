package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.bd.material.baseinfo.IMaterialBaseInfoQueryService;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialConvertVO;
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.material.stock.MaterialStockVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.org.DeptVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.model.entity.bill.AbstractBill;
import nc.jdbc.framework.processor.ArrayListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;

public class materialStockAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO Auto-generated method stub
		if (arg0 instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) arg0;
			Object[] newObjs = be.getNewObjs();
			List<Map> mapList = new ArrayList();
			String checkString = ((JSONArray)JSONObject.toJSON(mapList)).toString();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			IMaterialBaseInfoQueryService IQ = (IMaterialBaseInfoQueryService) NCLocator.getInstance().lookup(IMaterialBaseInfoQueryService.class);
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof MaterialStockVO) {
					MaterialStockVO sov = (MaterialStockVO)newObjs[i];
					String pk_material = sov.getPk_material();
					if(checkString!=null&&checkString.contains(pk_material))
						continue;
					String defaultWarehouseSrcSystemId = sov.getPk_stordoc();
					String[] mpks = {pk_material};
					MaterialVO[] mvos = IQ.queryDataByPks(mpks);
					MaterialVO ov = mvos[0];
					String code = ov.getCode();
					String name = ov.getName();
					String isWmsUnit = ov.getDef9();
					if(isWmsUnit!=null&&isWmsUnit.equals("Y"))
						isWmsUnit = "1";
					else
						isWmsUnit = "0";
					int enablestate = ov.getEnablestate();
					if(enablestate==1)
						enablestate=0;
					else if(enablestate==2)
						enablestate=1;
					else
						enablestate=2;
					String fix1 = (String)dao.executeQuery("select f.fix1 from bd_material m inner join bd_marasstframe f on m.pk_marasstframe=f.pk_marasstframe " + 
							"where m.pk_material='"+pk_material+"'", new ColumnProcessor());
					String isProductStatus = "0";
					if(fix1!=null&&fix1.equals("Y"))
						isProductStatus = "1";
					String pk_org = sov.getPk_org();
					String wmsFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
					if(wmsFlag==null||wmsFlag.equals("N"))
						continue;
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());

					String auxiliaryUnitId = (String)dao.executeQuery("select mc.pk_measdoc from bd_materialconvert mc where mc.pk_material='"+pk_material+"' and mc.isstockmeasdoc='Y'", new ColumnProcessor());
					String userId = InvocationInfoProxy.getInstance().getUserId();
					String psncode = (String)dao.executeQuery("select p.code from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where u.cuserid='"+userId+"'", new ColumnProcessor());
					String measrate = (String)dao.executeQuery("select mc.measrate from bd_materialconvert mc where mc.pk_material='"+pk_material+"' and mc.isstockmeasdoc='Y'", new ColumnProcessor());
					String specification = ov.getMaterialspec();
					String model = ov.getMaterialtype();
					String graphid = ov.getGraphid();
					String remark = ov.getMemo();
					String shortName = ov.getMaterialshortname();
					UFDouble unitLength = ov.getUnitlength()==null?null:new UFDouble(ov.getUnitlength());
					UFDouble unitHeight = ov.getUnitheight()==null?null:new UFDouble(ov.getUnitheight());
					UFDouble unitWidth = ov.getUnitwidth()==null?null:new UFDouble(ov.getUnitwidth());
					String unitWeight = ov.getUnitweight()==null?"":ov.getUnitweight().toString();
					String unitVolume = ov.getUnitvolume()==null?"":ov.getUnitvolume().toString();
					Map map = new HashMap();
					map.put("code", code);
					map.put("name", name);
					map.put("englishName", ov.getEname());
					map.put("englishSpecification", ov.getEmaterialspec());
					map.put("enableStatus", enablestate);
					map.put("orgCode", orgCode);
					map.put("orgName", orgName);
					map.put("baseOrgSrcSystemId", pk_org);
					map.put("productTypeSrcSystemId", ov.getPk_marbasclass());
					map.put("mainUnitSrcSystemId", ov.getPk_measdoc());
					map.put("auxiliaryUnitSrcSystemId", auxiliaryUnitId);
					map.put("defaultWarehouseSrcSystemId",defaultWarehouseSrcSystemId);
					map.put("conversionRate", measrate);
					map.put("specification", specification);
					map.put("model", model);
					map.put("graphid", graphid);
					map.put("remark", remark);
					map.put("shortName", shortName);
					map.put("unitLength", unitLength);
					map.put("unitHeight", unitHeight);
					map.put("unitWidth", unitWidth);
					map.put("unitWeight", unitWeight);
					map.put("unitVolume", unitVolume);
					map.put("description", ov.getMemo());
					map.put("inTolerance", ov.getIntolerance().toString());
					map.put("outTolerance", ov.getOuttolerance().toString());
					map.put("sourceId", pk_material);
					map.put("sourceType", 2);
					map.put("syncTime",(new UFDate(System.currentTimeMillis())).toString());
					map.put("syncBy",psncode);
					map.put("isProductStatus",isProductStatus);
					map.put("isWmsUnit",isWmsUnit);
					map.put("srcSystem", "NC");
					map.put("srcSystemId", pk_material);
					map.put("srcSystemCode", code);
					map.put("srcSystemName", name);
					map.put("delFlag", 0);
					
					String sql = "select s.wholemanaflag,s.qualitymanflag,(case when s.qualityunit=0 then s.qualitynum*365 else (case when s.qualityunit=1 then s.qualitynum*30 else s.qualitynum end) end) qualitynum,s.chkfreeflag,s.pk_org,s.pk_materialstock,s.pk_stordoc from bd_materialstock s where s.pk_material='"+pk_material+"' and s.dr=0";
					List<Object[]> results = (List<Object[]>) dao.executeQuery(sql, new ArrayListProcessor());
					List<Map> mapList2 = new ArrayList();
					for(int k=0;k<results.size();k++) {
						Map map_msv = new HashMap();
						map_msv.put("isBatch", String.valueOf(results.get(k)[0]).replace("Y", "1").replace("N", "0"));
						map_msv.put("isShelfLife", String.valueOf(results.get(k)[1]).replace("Y", "1").replace("N", "0"));
						map_msv.put("shelfLife", results.get(k)[2]==null?"":String.valueOf(results.get(k)[2]));
						map_msv.put("isExemption", String.valueOf(results.get(k)[3]).replace("Y", "1").replace("N", "0"));
						map_msv.put("baseOrgSrcSystemId", String.valueOf(results.get(k)[4]));
						map_msv.put("defaultWarehouseSrcSystemId",results.get(k)[6]==null?"":String.valueOf(results.get(k)[6]));
						map_msv.put("srcSystemId", String.valueOf(results.get(k)[5]));
						map_msv.put("srcSystem", "NC");
						map_msv.put("srcSystemName", name);
						map_msv.put("srcSystemCode", code);
						mapList2.add(map_msv);
					}
					map.put("featureList",mapList2);
					MaterialConvertVO[] cvs = ov.getMaterialconvert();
					int cl = 0;
					if(cvs!=null)
						cl = cvs.length;
					List<Map> mapList3 = new ArrayList();
					for(int j=0;j<cl;j++) {
						Map map_cov = new HashMap();
						String pk_measdoc = cvs[j].getPk_measdoc();
						String meacode = (String)dao.executeQuery("select m.code from bd_measdoc m where m.pk_measdoc='"+pk_measdoc+"'", new ColumnProcessor());
						String meaname = (String)dao.executeQuery("select m.name from bd_measdoc m where m.pk_measdoc='"+pk_measdoc+"'", new ColumnProcessor());
						map_cov.put("code", meacode);
						map_cov.put("name", meaname);
						map_cov.put("orgCode", orgCode);
						map_cov.put("orgName", orgName);
						map_cov.put("enableStatus",1);
						map_cov.put("sourceType", 2);
						map_cov.put("syncTime", (new UFDate(System.currentTimeMillis())).toString());
						map_cov.put("syncBy", psncode);
						map_cov.put("auxiliaryUnitSrcSystemId", cvs[j].getPk_measdoc());
						map_cov.put("isFixedConversion", cvs[j].getFixedflag().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("measRate", cvs[j].getMeasrate());
						map_cov.put("isBalance", cvs[j].getIsstorebalance().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("isPurchaseDefaultUnit", cvs[j].getIspumeasdoc().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("isProduceDefaultUnit", cvs[j].getIsprodmeasdoc().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("isStockDefaultUnit", cvs[j].getIsstockmeasdoc().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("isSalesDefaultUnit", cvs[j].getIssalemeasdoc().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("isRetailDefaultUnit", cvs[j].getIsretailmeasdoc().toString().replace("Y", "1").replace("N", "0"));
						map_cov.put("srcSystemId", cvs[j].getPk_materialconvert());
						map_cov.put("srcSystem", "NC");
						map_cov.put("srcSystemName", meaname);
						map_cov.put("srcSystemCode", meacode);
						mapList3.add(map_cov);
					}
					map.put("auxiliaryUnitAssociationList", mapList3);
					
					mapList.add(map);
				}
			}
			if(mapList!=null&&mapList.size()>0) {
				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//				String jsons = js.toString().substring(1, js.toString().length()-1);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/base/product/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "bd_material", "NC同步WMS物料");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "bd_material", "NC同步WMS物料");
					throw new BusinessException(e.toString());
				}
			}
		}
	}

}
