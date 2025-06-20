package mod.bobur;

import static com.besome.sketch.design.DesignActivity.sc_id;
import static com.besome.sketch.editor.LogicEditorActivity.getAllJavaFileNames;
import static com.besome.sketch.editor.LogicEditorActivity.getAllXmlFileNames;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import a.a.a.eC;
import a.a.a.jC;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.StringEditorBinding;
import pro.sketchware.databinding.StringEditorItemBinding;
import pro.sketchware.databinding.ViewStringEditorAddBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.XmlUtil;

public class StringEditorActivity extends BaseAppCompatActivity {

    private final ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
    private StringEditorBinding binding;
    private RecyclerViewAdapter adapter;
    private boolean isComingFromSrcCodeEditor = true;

    public static void convertXmlToListMap(
            String xmlString, ArrayList<HashMap<String, Object>> listmap) {
        try {
            listmap.clear();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input =
                    new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(new InputSource(input));
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("string");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    HashMap<String, Object> map = getStringHashMap((Element) node);
                    listmap.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, Object> getStringHashMap(Element node) {
        HashMap<String, Object> map = new HashMap<>();
        String key = node.getAttribute("name");
        String value = node.getTextContent().replace("\\", "");
        map.put("key", key);
        map.put("text", value);
        return map;
    }

    public static boolean isXmlStringsContains(
            ArrayList<HashMap<String, Object>> listMap, String value) {
        for (Map<String, Object> map : listMap) {
            if (map.containsKey("key") && value.equals(map.get("key"))) {
                return true;
            }
        }
        return false;
    }

    public static String convertListMapToXml(ArrayList<HashMap<String, Object>> listmap) {
        StringBuilder xmlString = new StringBuilder();
        xmlString.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
        for (HashMap<String, Object> map : listmap) {
            String key = (String) map.get("key");
            Object textObj = map.get("text");
            String text = textObj instanceof String ? (String) textObj : textObj.toString();
            String escapedText = escapeXml(text);
            xmlString.append("    <string name=\"").append(key).append("\"");
            if (map.containsKey("translatable")) {
                String translatable = (String) map.get("translatable");
                xmlString.append(" translatable=\"").append(translatable).append("\"");
            }
            xmlString.append(">").append(escapedText).append("</string>\n");
        }
        xmlString.append("</resources>");
        return xmlString.toString();
    }

    public static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'")
                .replace("\n", "&#10;")
                .replace("\r", "&#13;");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = StringEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.recyclerView,
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            v.getPaddingLeft(),
                            v.getPaddingTop(),
                            v.getPaddingRight(),
                            systemBars.bottom);
                    return insets;
                });
        initialize();
    }

    private void initialize() {
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(_v -> onBackPressed());
        binding.addStringButton.setOnClickListener(view -> addStringDialog());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        if (dy < 0) {
                            if (!binding.addStringButton.isExtended()) {
                                binding.addStringButton.extend();
                            }
                        } else if (dy > 0) {
                            if (binding.addStringButton.isExtended()) {
                                binding.addStringButton.shrink();
                            }
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isComingFromSrcCodeEditor) {
            convertXmlToListMap(FileUtil.readFile(getIntent().getStringExtra("content")), listmap);
            adapter = new RecyclerViewAdapter(listmap);
            binding.recyclerView.setAdapter(adapter);
        }
        isComingFromSrcCodeEditor = false;
    }

    @Override
    public void onBackPressed() {
        ArrayList<HashMap<String, Object>> cache = new ArrayList<>();
        convertXmlToListMap(FileUtil.readFile(getIntent().getStringExtra("content")), cache);
        String cacheString = new Gson().toJson(cache);
        String cacheListmap = new Gson().toJson(listmap);
        if (cacheListmap.equals(cacheString) || listmap.isEmpty()) {
            setResult(RESULT_OK);
            finish();
        } else {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(Helper.getResString(R.string.common_word_warning));
            dialog.setMessage(Helper.getResString(
                    R.string.src_code_editor_unsaved_changes_dialog_warning_message));
            dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (v, which) -> {
                XmlUtil.saveXml(
                        getIntent().getStringExtra("content"),
                        convertListMapToXml(listmap));
                v.dismiss();
                finish();
            });
            dialog.setNegativeButton(Helper.getResString(R.string.common_word_exit), (v, which) -> {
                v.dismiss();
                finish();
            });
            dialog.show();
        }
        if (listmap.isEmpty()
                && (!FileUtil.readFile(getIntent().getStringExtra("content"))
                .contains("</resources>"))) {
            XmlUtil.saveXml(getIntent().getStringExtra("content"), convertListMapToXml(listmap));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.string_editor_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(
                    new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextChange(String newText) {
                            adapter.filter(newText);
                            return false;
                        }

                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return false;
                        }
                    });
        }

        menu.findItem(R.id.action_get_default)
                .setVisible(!checkDefaultString(getIntent().getStringExtra("content")));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            XmlUtil.saveXml(getIntent().getStringExtra("content"), convertListMapToXml(listmap));
        } else if (id == R.id.action_get_default) {
            convertXmlToListMap(
                    FileUtil.readFile(
                            getDefaultStringPath(
                                    Objects.requireNonNull(getIntent().getStringExtra("content")))),
                    listmap);
            adapter.notifyDataSetChanged();
        } else if (id == R.id.action_open_editor) {
            isComingFromSrcCodeEditor = true;
            XmlUtil.saveXml(getIntent().getStringExtra("content"), convertListMapToXml(listmap));
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), SrcCodeEditor.class);
            intent.putExtra("title", getIntent().getStringExtra("title"));
            intent.putExtra("content", getIntent().getStringExtra("content"));
            intent.putExtra("xml", getIntent().getStringExtra("xml"));
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void addStringDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        ViewStringEditorAddBinding binding =
                ViewStringEditorAddBinding.inflate(LayoutInflater.from(this));
        dialog.setTitle("Create new string");
        dialog.setPositiveButton("Create", (v1, which) -> {
            String key =
                    Objects.requireNonNull(binding.stringKeyInput.getText()).toString();
            String value =
                    Objects.requireNonNull(binding.stringValueInput.getText()).toString();

            if (key.isEmpty() || value.isEmpty()) {
                SketchwareUtil.toast("Please fill in all fields", Toast.LENGTH_SHORT);
                return;
            }

            if (isXmlStringsContains(listmap, key)) {
                binding.stringKeyInputLayout.setError("\"" + key + "\" is already exist");
                return;
            }
            addString(key, value);
        });
        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
    }

    public void addString(String key, String text) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("text", text);
        if (listmap.isEmpty()) {
            listmap.add(map);
            adapter.notifyItemInserted(0);
            return;
        }
        for (int i = 0; i < listmap.size(); i++) {
            if (Objects.equals(listmap.get(i).get("key"), key)) {
                listmap.set(i, map);
                adapter.notifyItemChanged(i);
                return;
            }
        }
        listmap.add(map);
        adapter.notifyItemInserted(listmap.size() - 1);
    }

    public boolean checkDefaultString(String path) {
        File file = new File(path);
        String parentFolder = Objects.requireNonNull(file.getParentFile()).getName();
        return parentFolder.equals("values");
    }

    public String getDefaultStringPath(String path) {
        return path.replaceFirst("/values-[a-z]{2}", "/values");
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> originalData;
        private ArrayList<HashMap<String, Object>> filteredData;

        public RecyclerViewAdapter(ArrayList<HashMap<String, Object>> data) {
            originalData = new ArrayList<>(data);
            filteredData = data;
        }

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            StringEditorItemBinding itemBinding =
                    StringEditorItemBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
            HashMap<String, Object> item = filteredData.get(position);
            String key = (String) item.get("key");
            String text = (String) item.get("text");
            holder.binding.textInputLayout.setHint(key);
            holder.binding.editText.setText(text);

            holder.binding.editText.setOnClickListener(
                    v -> {
                        int adapterPosition = holder.getAbsoluteAdapterPosition();
                        HashMap<String, Object> currentItem = filteredData.get(adapterPosition);

                        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(StringEditorActivity.this);
                        ViewStringEditorAddBinding dialogBinding =
                                ViewStringEditorAddBinding.inflate(
                                        LayoutInflater.from(StringEditorActivity.this));

                        dialogBinding.stringKeyInput.setText((String) currentItem.get("key"));
                        dialogBinding.stringValueInput.setText((String) currentItem.get("text"));

                        dialog.setTitle("Edit string");
                        dialog.setPositiveButton("Save", (v1, which) -> {
                            String keyInput =
                                    Objects.requireNonNull(
                                                    dialogBinding.stringKeyInput.getText())
                                            .toString();
                            String valueInput =
                                    Objects.requireNonNull(
                                                    dialogBinding.stringValueInput
                                                            .getText())
                                            .toString();
                            if (keyInput.isEmpty() || valueInput.isEmpty()) {
                                SketchwareUtil.toast(
                                        "Please fill in all fields", Toast.LENGTH_SHORT);
                                return;
                            }
                            if (keyInput.equals(key) && valueInput.equals(text)) {
                                return;
                            }
                            currentItem.put("key", keyInput);
                            currentItem.put("text", valueInput);
                            notifyItemChanged(adapterPosition);
                        });

                        dialog.setNeutralButton("Delete", (v1, which) -> {
                            if (isXmlStringUsed(key)) {
                                SketchwareUtil.toastError(
                                        Helper.getResString(
                                                R.string
                                                        .logic_editor_title_remove_xml_string_error));
                            } else {
                                filteredData.remove(adapterPosition);
                                notifyItemRemoved(adapterPosition);
                            }
                        });
                        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
                        dialog.setView(dialogBinding.getRoot());
                        dialog.show();
                    });
        }

        @Override
        public int getItemCount() {
            return filteredData.size();
        }

        /**
         * Filters the data based on the query.
         *
         * @param query The search query.
         */
        public void filter(String query) {
            if (query == null || query.isEmpty()) {
                filteredData = new ArrayList<>(originalData);
            } else {
                filteredData = new ArrayList<>();
                for (HashMap<String, Object> item : originalData) {
                    String key = (String) item.get("key");
                    String text = (String) item.get("text");
                    if ((key != null && key.toLowerCase().contains(query.toLowerCase()))
                            || (text != null && text.toLowerCase().contains(query.toLowerCase()))) {
                        filteredData.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public boolean isXmlStringUsed(String key) {
            if ("app_name".equals(key) || sc_id == null) {
                return false;
            }

            String projectScId = sc_id;
            eC projectDataManager = jC.a(projectScId);

            return isStringUsedInJavaFiles(projectScId, projectDataManager, key)
                    || isStringUsedInXmlFiles(projectScId, projectDataManager, key);
        }

        private boolean isStringUsedInJavaFiles(
                String projectScId, eC projectDataManager, String key) {
            for (String javaFileName : getAllJavaFileNames(projectScId)) {
                for (Map.Entry<String, ArrayList<BlockBean>> entry :
                        projectDataManager.b(javaFileName).entrySet()) {
                    for (BlockBean block : entry.getValue()) {
                        if ((block.opCode.equals("getResStr") && block.spec.equals(key))
                                || (block.opCode.equals("getResString")
                                && block.parameters.get(0).equals("R.string." + key))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean isStringUsedInXmlFiles(
                String projectScId, eC projectDataManager, String key) {
            for (String xmlFileName : getAllXmlFileNames(projectScId)) {
                for (ViewBean view : projectDataManager.d(xmlFileName)) {
                    if (view.text.text.equals("@string/" + key)
                            || view.text.hint.equals("@string/" + key)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            StringEditorItemBinding binding;

            public ViewHolder(StringEditorItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
