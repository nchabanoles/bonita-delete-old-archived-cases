package bonita.deleteCases.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

public class TestAPI {
	public static void main(String args[]) {
		
		Logger logger = Logger.getLogger("DeleteArchivedCases");
		try {

			Map<String, String> map = new HashMap<String, String>();
			map.put("server.url",args[0]);
			map.put("application.name", "bonita");
			APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);
			final String username = "POAdmin#1";
			final String password = "bpm";
			final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
			final APISession session = loginAPI.login(username, password);

			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);

			long nbDeletedProcessInstances = 0;
			int numberOfCases=Integer.valueOf(args[1]);

			Long millis = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000 );
			int iterations=Integer.valueOf(args[2]);

			
			do {
				logger.info("Deleting "+ numberOfCases+ " cases");
				long timeInMillis=Calendar.getInstance().getTimeInMillis();
				SearchOptionsBuilder sob = new SearchOptionsBuilder(0, numberOfCases);
				sob.sort(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, Order.ASC);
				
				SearchResult<ArchivedProcessInstance> sr = processAPI.searchArchivedProcessInstances(sob.done());

				List<Long> archivedProcessInstanceIds = sr.getResult().stream().filter(ap->ap.getArchiveDate().getTime()<millis).map(ap -> ap.getSourceObjectId())
						.collect(Collectors.toList());
				
				logger.info(archivedProcessInstanceIds.size() +" cases found");

				nbDeletedProcessInstances = processAPI
						.deleteArchivedProcessInstancesInAllStates(archivedProcessInstanceIds);

				double elapsedTime=(Calendar.getInstance().getTimeInMillis()-timeInMillis)/1000;
				logger.info(elapsedTime + " seconds elapsed to delete the cases");
				
			} while (nbDeletedProcessInstances > 0&&iterations-->0);

			loginAPI.logout(session);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "ERROR RETRIEVING USERS", e);
		}
	}
}