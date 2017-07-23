import gurobi.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolveLP {

    public static void main(String[] args) throws Exception {

        String fileName = "Graph2";
        float fileSize = 200;
        int numOfCommodities = 1;


        List<String[]> Capacities = new ArrayList<>();
        HashMap<String, List<String>> adj_list_out = new HashMap<>();
        HashMap<String, List<String>> adj_list_in = new HashMap<>();
        List<GRBVar> flow_variables = new ArrayList<>();
        HashMap<String, Integer> mapping = new HashMap<>();


        // Model
        GRBEnv env = new GRBEnv("MCF.log");
        GRBModel model = new GRBModel(env);

        String[] source_sink = new String[numOfCommodities];
        source_sink[0] = "1,4";
//        source_sink[1] = "2,12";



        BufferedReader BR = new BufferedReader(new FileReader(fileName));
        String line;

        while ((line = BR.readLine()) != null) {

            String[] tmp = line.split(",");
            String n1 = tmp[0];
            String n2 = tmp[1];

            Capacities.add(new String[]{n1, n2, ""+Integer.parseInt(tmp[2])/fileSize});


            if (adj_list_out.containsKey(n1))
                adj_list_out.get(n1).add(n2);
            else {
                List<String> tmp2 = new ArrayList<>();
                tmp2.add(n2);
                adj_list_out.put(n1, tmp2);
            }


            if (adj_list_in.containsKey(n2))
                adj_list_in.get(n2).add(n1);
            else {
                List<String> tmp2 = new ArrayList<>();
                tmp2.add(n1);
                adj_list_in.put(n2, tmp2);
            }


            for (int p = 0; p < numOfCommodities; ++p) {
                GRBVar tmp_flow = model.addVar(0, 1, 0, GRB.CONTINUOUS, "f_"+n1+"-"+n2+"-"+p);
                flow_variables.add(tmp_flow);
                mapping.put("f_"+n1+"-"+n2+"-"+p, flow_variables.size()-1);
            }
        }

        model.update();

        GRBVar[] flows = new GRBVar[2];
        for (int p = 0; p < numOfCommodities; ++p) {
            flows[p] = model.addVar(0, 1, 0, GRB.CONTINUOUS, "f2_"+p);
        }


        model.update();

        // Objective Function
        GRBLinExpr expr = new GRBLinExpr();
        for (int p = 0; p < numOfCommodities; ++p) {
            expr.addTerm(1.0, model.getVarByName("f2_"+p));
//            expr.addTerm(1.0, flows[p]);

        }
        model.setObjective(expr, GRB.MAXIMIZE);

        model.update();

        // Add constraint 0
        for (int i = 0; i != numOfCommodities; i++) {

            String source = source_sink[i].split(",")[0];
            List<String> neighbors = adj_list_out.get(source);

            GRBLinExpr expr2 = new GRBLinExpr();
            for(int j = 0; j != neighbors.size(); j++) {
                expr2.addTerm(1.0, model.getVarByName("f_"+source+"-"+neighbors.get(j)+"-"+i));
//                expr2.addTerm(1.0, flow_variables.get(mapping.get("f_"+source+"-"+neighbors.get(j)+"-"+i)));
            }

            model.addConstr(expr2, GRB.EQUAL, model.getVarByName("f2_"+i), "c0_"+i);
//            model.addConstr(expr2, GRB.EQUAL, flows[i], "c0_"+i);
        }

        model.update();


        // Add constraint 1
        for (int i = 0; i != numOfCommodities; i++) {

            GRBLinExpr expr_inFlow = new GRBLinExpr();
            GRBLinExpr expr_outFlow = new GRBLinExpr();
            String currentCommodity_source = source_sink[i].split(",")[0];
            String currentCommodity_sink = source_sink[i].split(",")[1];

            for (Map.Entry<String, List<String>> entry : adj_list_out.entrySet()) {
                String current = entry.getKey();
                if (current.equals(currentCommodity_source) || current.equals(currentCommodity_sink) || !adj_list_in.containsKey(current))
                    continue;

                List<String> tmp_outFlow = entry.getValue();
                for (int k = 0; k != tmp_outFlow.size(); k++) {
                    String to = tmp_outFlow.get(k);
                    expr_outFlow.addTerm(1.0, model.getVarByName("f_"+current+"-"+to+"-"+i));
//                    expr_outFlow.addTerm(1.0, flow_variables.get(mapping.get("f_"+current+"-"+to+"-"+i)));
                }

                List<String> tmp_inFlow = adj_list_in.get(current);
                for (int k = 0; k != tmp_inFlow.size(); k++) {
                    String from = tmp_inFlow.get(k);
                    expr_inFlow.addTerm(1.0, model.getVarByName("f_"+from+"-"+current+"-"+i));
//                    expr_outFlow.addTerm(1.0, flow_variables.get(mapping.get("f_"+from+"-"+current+"-"+i)));
                }

            }

            model.addConstr(expr_inFlow, GRB.EQUAL, expr_outFlow, "c1_"+i);

        }

        model.update();


        // Add constraint 2
        for (int k = 0; k != Capacities.size(); k++) {

            String n1 = Capacities.get(k)[0];
            String n2 = Capacities.get(k)[1];
            String cap = Capacities.get(k)[2];

            GRBLinExpr expr_edgeFlow = new GRBLinExpr();
            for (int i = 0; i != numOfCommodities; i++) {
                expr_edgeFlow.addTerm(1.0, model.getVarByName("f_"+n1+"-"+n2+"-"+i));
//                expr_edgeFlow.addTerm(1.0, flow_variables.get(mapping.get("f_"+n1+"-"+n2+"-"+i)));
            }

            model.addConstr(expr_edgeFlow, GRB.LESS_EQUAL, Float.parseFloat(cap), "c2_"+k);

        }

        model.update();

        model.optimize();
        System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));


        for (int k = 0; k != flow_variables.size(); k++) {
            System.out.println(flow_variables.get(k).get(GRB.StringAttr.VarName) + "\t" + flow_variables.get(k).get(GRB.DoubleAttr.X));
        }

        for (int i = 0; i != numOfCommodities; i++) {
            System.out.println(flows[i].get(GRB.StringAttr.VarName) + "\t" + flows[i].get(GRB.DoubleAttr.X));
        }


        // Dispose of model and environment
        model.dispose();
        env.dispose();

    }

}
