package bonita.deleteCases.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.Session;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

public class TestAPI {

    /*static final String username = "jan.fisher";
    static final String password = "bpm";*/

/*    static final String username = "install";
    static final String password = "install";*/

    static final String username = "walter.bates";
    static final String password = "bpm";


    private static final long ONE_MONTH = 30l * 24l * 60l * 60l * 1000l;

    public static void main(String args[]) {
		
		Logger logger = Logger.getLogger("DeleteArchivedCases");

		String serverURL = "http://localhost:" + "11886";
        int pageSize = Integer.valueOf(args[1]);
        int pageNumber = Integer.valueOf(args[2]);

        int numberOfCasesToDelete=pageSize * pageNumber;
        logger.info("You asked me to delete at most "+ numberOfCasesToDelete+ " cases.");

		try {

		    // Personal preference to use APIClient as I find it easier to test
            buildClientForHTTPServer(serverURL);

            LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
            APISession session = loginAPI.login(username, password);

            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);

			Long lastMonth = System.currentTimeMillis() - ONE_MONTH;
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, pageSize);
            sob.sort(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, Order.ASC);
            SearchOptions options = sob.done();


            List<Long> idsOfCasesToDelete;

			do {

				long startTime=System.currentTimeMillis();

				SearchResult<ArchivedProcessInstance> sr = processAPI.searchArchivedProcessInstances(options);

				idsOfCasesToDelete = sr.getResult().stream().map(ap -> ap.getSourceObjectId())
						.collect(Collectors.toList());
				
				logger.info(idsOfCasesToDelete.size() +" cases found");

				if(idsOfCasesToDelete.size() > 0) {
                    numberOfCasesToDelete -= (int) processAPI
                            .deleteArchivedProcessInstancesInAllStates(idsOfCasesToDelete);

                    double elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    logger.info(elapsedTime + " seconds elapsed to delete the cases");
                }
				
			} while (numberOfCasesToDelete > 0 && idsOfCasesToDelete.size()>0); // Iterate until we deleted the requested number of cases or there is no more older than last month

			loginAPI.logout(session);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "ERROR Deleting archived processes", e);
		}
	}

    private static APIClient buildClientForHTTPServer(String serverURL) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("server.url", serverURL);
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);
        return new APIClient();
    }
}