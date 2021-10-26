import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class classify {
    public static void main(String[] args){
        Node decisionTree = getDecisionTreeFromFile("agaricus-lepiota.json");
        ArrayList<ArrayList<String>> D = getData("agaricus-lepiota.csv");
        classifyAllInputs(D, decisionTree);
    }

    public static ArrayList<String> run(JSONObject json, List<ArrayList<String>> holdoutSet){
        JSONObject treeJson = json.getJSONObject("node");
        Node decisionTree = buildTree(treeJson);
        classifyAllInputs(holdoutSet, decisionTree);
        return null;
    }

    public static Node getDecisionTreeFromFile(String fileName){
        InputStream inputStream = classify.class.getResourceAsStream(fileName);
        if(inputStream == null){
            return null;
        }

        JSONTokener tokener = new JSONTokener(inputStream);
        JSONObject json = new JSONObject(tokener);
        try{
            JSONObject tree = json.getJSONObject("node");
            return buildTree(tree);
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Node buildTree(JSONObject json){
        if(!json.isNull("decision")){
            return new Node("", json.getString("decision"), null, json.getDouble("p"));
        }
        else {
            Node n = new Node(json.getString("var"), "", new ArrayList<Edge>(), 0);
            JSONArray edges = json.getJSONArray("edges");
            for(int i = 0; i<edges.length(); i++){
                JSONObject edge = edges.getJSONObject(i).getJSONObject("edge");
                if(edge.isNull("leaf")){
                    n.addEdge(edge.getString("value"), buildTree(edge.getJSONObject("node")));
                } else{
                    n.addEdge(edge.getString("value"), buildTree(edge.getJSONObject("leaf")));
                }
            }
            return n;
        }
    }

    public static void classifyAllInputs(List<ArrayList<String>> D, Node tree){
        ArrayList<String> classifications = new ArrayList<>();
        ArrayList<String> attributeKey = D.get(0);
        String classVar = D.get(2).get(0);
        D.remove(0);
        D.remove(0);
        D.remove(0);
        for(ArrayList<String> point : D){
            classifications.add(classifyDataPoint(point, attributeKey, tree));
        }
        if(classVar.equals("")){
            System.out.println(classifications.toString());
            return;
        }
        int classVarLoc = -1;
        for(int i=0; i<attributeKey.size(); i++){
            if(classVar.equals(attributeKey.get(i))){
                classVarLoc = i;
                break;
            }
        }
        int numCorrect = 0;
        for(int i=0; i<D.size(); i++){
            if(D.get(i).get(classVarLoc).equals(classifications.get(i))){
                numCorrect += 1;
            }
        }
        System.out.println("Total records classified: "+D.size());
        System.out.println("Total correct: " + numCorrect);
        System.out.println("Total incorrect: " + (D.size() - numCorrect));
        System.out.println("Accuracy: " + (Double.valueOf(numCorrect) / Double.valueOf(D.size())));
    }

    public static String classifyDataPoint(ArrayList<String> point, ArrayList<String> attributekey, Node tree){
        Node currNode = tree;
        // System.out.println(point);
        while(currNode.decision==null){
            // System.out.println(currNode.toJSON());
            int attIdx = -1;
            for(int i=0; i<attributekey.size(); i++){
                if(currNode.attribute.equals(attributekey.get(i))){
                    attIdx = i;
                    break;
                }
            }
            String pointData = point.get(attIdx);
            Node ghostTree;
            for(Edge e : currNode.edges){
                if(e.edge.equals(pointData)){
                    currNode = e.next;
                    break;
                } else if(e.edge.equals("ghost")){
                    currNode = e.next;
                }
            }
            
        }
        // System.out.println(currNode.decision);
        return currNode.decision;
    }

    public static ArrayList<ArrayList<String>> getData(String path){
        Scanner sc;
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        try {
            sc = new Scanner(new File(path));
            while (sc.hasNextLine()){
                ArrayList<String> lineVals = new ArrayList<>();
                String[] line = sc.nextLine().split(",");
                for(String s : line){
                    lineVals.add(s);
                }
                data.add(lineVals);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }
}
