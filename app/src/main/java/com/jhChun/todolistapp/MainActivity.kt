package com.jhChun.todolistapp

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jhChun.todolistapp.databinding.ActivityMainBinding
import com.jhChun.todolistapp.databinding.ItemTodoBinding

class MainActivity : AppCompatActivity() {

    val RC_SIGN_IN = 1000

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /* 파이어베이스 이메일 로그인 기능 사용 START */

        // Initialize Firebase Auth
        // 로그인이 안된 경우
        if(FirebaseAuth.getInstance().currentUser == null) {

        }

        /* 파이어베이스 이메일 로그인 기능 사용 E N D */

//        data.add(Todo("헬스"))
//        data.add(Todo("안드로이드 강의"))

//        binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        binding.recyclerView.adapter = TodoAdapter(data,
//            // onClickDeleteIcon 버튼 클릭 시, 아래 함수가 실행
//            onClickDeleteIcon = {
//                deleteTodo(it)
//            }
//        )
        // 위처럼 binding.recyclerView 가 반복되는 경우 아래와 같이 apply 객체에 담아서 한꺼번에 실행시킬 수 있음

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = TodoAdapter(
                emptyList(),
                // onClickDeleteIcon 버튼 클릭 시, 아래 함수가 실행
                onClickDeleteIcon = {
                    viewModel.deleteTodo(it)
                    // 데이타 변경 후 어댑터에 변경사항을 노티
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                },
                onClickItem = {
                    viewModel.toggleTodo(it)
                    // 데이타 변경 후 어댑터에 변경사항을 노티.
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
            )
        }

        binding.addButton.setOnClickListener {
            val todo = Todo(binding.editText.text.toString())
            // 텍스트뷰에 문자열이 존재할 때만 추가버튼 실행
            if(binding.editText.text.isNotEmpty()){
                viewModel.addTodo(todo)
                // 데이타 변경 후 어댑터에 변경사항을 노티
                binding.recyclerView.adapter?.notifyDataSetChanged()
            }
            binding.editText.setText("") // 추가 버튼 클릭 이후 남아있는 문자열 제거
        }

        // 관찰 UI 업데이트
        viewModel.todoLiveData.observe(this, Observer {
            (binding.recyclerView.adapter as TodoAdapter).setData(it)
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                //val user = FirebaseAuth.getInstance().currentUser
                // 로그인 성공
                viewModel.fetchData()
            } else {
                // 로그인 실패
                finish()
            }
        }
    }

    fun login() {
        // Choose authentication providers
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                // 로그아웃 성공 시 수행
                login()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.action_log_out -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// 데이터 클래스 : 자동으로 겟터, 셋터 구현됨. 모델클래스로 사용 가능.
data class Todo(
    val text: String,
    var isDone: Boolean = false
)


// 어댑터: 리사이클러뷰 안에 데이터를 어떤식으로 표현할지 정의하는 것
class TodoAdapter(
    private var dataSet: List<Todo>,
    // 아래는 함수임. 인풋: todo, 아웃풋은 없음
    val onClickDeleteIcon: (todo: Todo) -> Unit,
    val onClickItem: (todo: Todo) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    // 모든 바인딩 객체는 루트라는 프로퍼티가 있어서, 본인이 어떤 뷰로부터 생성된 바인딩인지에 대한 정보를 루트에 가지고 있다.
    // ItemTodoBinding.bind(view) 으로 리턴된 view가 RecyclerView.ViewHolder(binding.root) 에서 얻어진다.
    class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_todo, viewGroup, false)

        return TodoViewHolder(ItemTodoBinding.bind(view))
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = dataSet[position]
        // binding을 통해 findViewById를 사용할 필요가 없어졌음
        //val textView = holder.view.findViewById<TextView>(R.id.todo_text)
        holder.binding.todoText.text = todo.text

        if (todo.isDone) {
            //holder.binding.todoText.paintFlags = holder.binding.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.todoText.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTypeface(null, Typeface.ITALIC)
            }
        } else {
            holder.binding.todoText.apply {
                paintFlags = 0 // 노멀값
                setTypeface(null, Typeface.NORMAL)
            }
        }

        holder.binding.deleteImageView.setOnClickListener {
            onClickDeleteIcon.invoke(todo)
        }

        holder.binding.root.setOnClickListener {
            onClickItem.invoke(todo)
        }

    }

    override fun getItemCount() = dataSet.size

    fun setData(newData:List<Todo>){
        dataSet = newData
        notifyDataSetChanged()
    }

}

// 데이타를 액티비티가 아닌 뷰모델이 관리를 하게 하기 위해 클래스 생성 -> 데이타 관련 코드를 이쪽에서 관리
class MainViewModel: ViewModel() {

    val db = Firebase.firestore

    /* 라이브데이타: 상태 변경이 가능하고 조회가 가능
       라이브데이타 사용의 장점:
        - 화면 갱신 코드를 한쪽에 따로 모아놓을 수 있어서 코드 관리가 용이 (매 부분마다 갱신코드를 분산시켜 넣지 않아도 됨)
    */
    val todoLiveData = MutableLiveData<List<Todo>>()
    private val data = arrayListOf<Todo>()
    
    // 초기화코드 작성
    init {
        fetchData()
    }

    fun fetchData() {
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            db.collection(user.uid)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    data.clear()
                    for (document in value!!) {
                        val todo = Todo(
                            document.getString("text")?:"", // null에 대한 처리로 인한 앱 크래쉬 방지
                            document.getBoolean("isDone")?:false // null에 대한 처리로 인한 앱 크래쉬 방지
                        )
                        data.add(todo)
                    }
                    todoLiveData.value = data // 데이타 변경이 있을 때마다 라이브데이타에 반영 -> viewModel.todoLiveData.observe 이 호출됨
                }

                // 아래의 코드는 위(실시간 리얼타임으로 동작 가능 -> 편리성 증가)와 같이 대체될 수 있음
                // DB가 변경되면 변경된 데이타가 앱에 실시간으로 반영됨 (타 프로그램에서 쌓아준 데이타가 실시간으로 반영되도록 구현이 가능한 듯)
//                .get()
//                .addOnSuccessListener { result ->
//                    data.clear()
//                    for (document in result) {
//                        val todo = Todo(
//                            // data : Map 으로 되어있으므로 Key를 지정하여 Value 를 조회함
//                            document.data["text"] as String,
//                            document.data["isDone"] as Boolean
//                        )
//                        data.add(todo)
//                    }
//                    todoLiveData.value = data // 데이타 변경이 있을 때마다 라이브데이타에 반영 -> viewModel.todoLiveData.observe 이 호출됨
                }
        }
    }

    fun toggleTodo(todo: Todo) {
        todo.isDone = !todo.isDone
    }

    fun addTodo(todo: Todo) {
        data.add(todo)
        todoLiveData.value = data // 데이타 변경이 있을 때마다 라이브데이타에 반영 -> viewModel.todoLiveData.observe 이 호출됨
    }

    fun deleteTodo(todo: Todo) {
        data.remove(todo)
        todoLiveData.value = data // 데이타 변경이 있을 때마다 라이브데이타에 반영 -> viewModel.todoLiveData.observe 이 호출됨
    }

}
