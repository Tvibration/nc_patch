package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.BusinessEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.general.businessevent.ICGeneralCommonEvent;
import nc.bs.wms.tool.WMSHttpTool;
import nc.bs.uapbd.tool.HttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.org.DeptVO;
import nc.vo.pu.m21.entity.OrderHeaderVO;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;

public class matoutAfterCancleSign implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO 自动生成的方法存根
		if (arg0 instanceof ICGeneralCommonEvent) {
			ICGeneralCommonEvent be = (ICGeneralCommonEvent) arg0;
			Object newObjs = be.getOldObjs();
			List<Map> mapList = new ArrayList();
//			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			if (newObjs instanceof MaterialOutVO[]) {
				MaterialOutVO[] ovs = (MaterialOutVO[])newObjs;
				for(int i=0;i<ovs.length;i++) {
					  MaterialOutVO cv = ovs[i];
					  MaterialOutBodyVO[] pbv = (MaterialOutBodyVO[])cv.getChildrenVO();
					  String csourcetype = pbv[0].getCsourcetype();
					  if(csourcetype!=null&&csourcetype.equals("4455")) {
						  MaterialOutHeadVO phv = cv.getHead();
						  String srcSystemId = phv.getCgeneralhid();
						  Map map = new HashMap();
						  map.put("srcSystemId",srcSystemId);
						  mapList.add(map);
						  JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
						  String jsons = js.toString();
						  String url = WMSHttpTool.getWMSURL()+"/stock/stock-material-out/removeBySrcSystemId/"+srcSystemId;
						  try {
								String res = WMSHttpTool.sendPost(jsons, url);
								if(res.contains("\"success\":true")) {
									NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "ic_material_h", "NC材料出库取消签字同步WMS");
								}else
									throw new BusinessException(res);
						  } catch (Exception e) {
								// TODO Auto-generated catch block
								NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "ic_material_h", "NC材料出库取消签字同步WMS");
								throw new BusinessException(e.toString());
						  }
					  }
					 
				  }
					  
				
			}		
		}     
	
	}

}
