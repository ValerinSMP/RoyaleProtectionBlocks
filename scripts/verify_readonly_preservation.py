import struct,zipfile,sys
class Reader:
 def __init__(self,data):self.data=data;self.pos=0;self.pool={}
 def read(self,n):x=self.data[self.pos:self.pos+n];self.pos+=n;return x
 def u1(self):return self.read(1)[0]
 def u2(self):return struct.unpack('>H',self.read(2))[0]
 def u4(self):return struct.unpack('>I',self.read(4))[0]
 def attrs(self):
  result={}
  for _ in range(self.u2()):
   name=self.pool[self.u2()];result[name]=self.read(self.u4())
  return result
 def members(self):
  result={}
  for _ in range(self.u2()):
   access=self.u2();name=self.pool[self.u2()];desc=self.pool[self.u2()];result[name+desc]=(access,self.attrs())
  return result
 def parse(self):
  assert self.u4()==0xcafebabe;self.read(4);n=self.u2();i=1
  while i<n:
   tag=self.u1()
   if tag==1:self.pool[i]=self.read(self.u2()).decode('utf8',errors='replace')
   elif tag in [3,4,9,10,11,12,17,18]:self.read(4)
   elif tag in [5,6]:self.read(8);i+=1
   elif tag in [7,8,16,19,20]:self.read(2)
   elif tag==15:self.read(3)
   else:raise AssertionError(tag)
   i+=1
  self.read(6);self.read(self.u2()*2)
  fields=self.members();methods=self.members();attrs=self.attrs();return fields,methods,attrs
path='royale/RoyaleProtectionBlocks/Plugin/Modules/PlayerInteractionsPckg/PlayerInteractionsServiceImpl.class'
with zipfile.ZipFile(sys.argv[1]) as a,zipfile.ZipFile(sys.argv[2]) as b:
 r1=Reader(a.read(path));r2=Reader(b.read(path));f1,m1,a1=r1.parse();f2,m2,a2=r2.parse()
 assert f1==f2 and a1==a2
 assert m1.keys()==m2.keys()
 target='checkIfWritable()V'
 for name in m1:
  if name!=target:assert m1[name]==m2[name],('Unrelated method changed',name)
 def code_data(raw,pool):
  r=Reader(raw);r.pool=pool
  header=r.read(4);code=r.read(r.u4());exceptions=r.read(r.u2()*8);attributes=r.attrs()
  return header,code,exceptions,attributes
 h1,before,e1,meta1=code_data(m1[target][1]['Code'],r1.pool)
 h2,after,e2,meta2=code_data(m2[target][1]['Code'],r2.pool)
 assert h1==h2 and e1==e2 and meta1==meta2
 assert len(before)==len(after)
 differences=[i for i in range(len(before)) if before[i]!=after[i]]
 assert 1<=len(differences)<=2
 start=min(differences)-1
 if before[start]!=0xb2:start-=1
 assert before[start]==0xb2 and set(differences)<={start+1,start+2}
 print('Read-only preservation:',len(m1)-1,'other methods, all fields and class attributes byte-identical; only GETSTATIC field index changed.')
