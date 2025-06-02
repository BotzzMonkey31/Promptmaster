<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '../services/api'
import Cookies from 'js-cookie'

const router = useRouter()

interface UserForm {
  email: string
  name: string
  picture: string
  username: string
  country: string
}

const userForm = ref<UserForm>({
  email: '',
  name: '',
  picture: '',
  username: '',
  country: '',
})

const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const userData = Cookies.get('user')
    if (!userData) {
      router.push('/login')
      return
    }

    const parsedUserData = JSON.parse(userData)

    userForm.value = {
      email: parsedUserData.email,
      name: parsedUserData.name,
      picture: parsedUserData.picture,
      username: '',
      country: '',
    }

    const response = await apiClient.get(`/users/check/${parsedUserData.email}`)

    if (response.data.data===true) {
      router.push('/')
    }

    isLoading.value = false
  } catch (err) {
    error.value = 'Error checking user status'
    console.error(err)
  }
})

const countries = [
  'Afghanistan',
  'Albania',
  'Algeria',
  'Andorra',
  'Angola',
  'Antigua and Barbuda',
  'Argentina',
  'Armenia',
  'Australia',
  'Austria',
  'Azerbaijan',
  'Bahamas',
  'Bahrain',
  'Bangladesh',
  'Barbados',
  'Belarus',
  'Belgium',
  'Belize',
  'Benin',
  'Bhutan',
  'Bolivia',
  'Bosnia and Herzegovina',
  'Botswana',
  'Brazil',
  'Brunei',
  'Bulgaria',
  'Burkina Faso',
  'Burundi',
  'Cabo Verde',
  'Cambodia',
  'Cameroon',
  'Canada',
  'Central African Republic',
  'Chad',
  'Chile',
  'China',
  'Colombia',
  'Comoros',
  'Congo',
  'Costa Rica',
  'Croatia',
  'Cuba',
  'Cyprus',
  'Czech Republic',
  'Democratic Republic of the Congo',
  'Denmark',
  'Djibouti',
  'Dominica',
  'Dominican Republic',
  'Ecuador',
  'Egypt',
  'El Salvador',
  'Equatorial Guinea',
  'Eritrea',
  'Estonia',
  'Eswatini',
  'Ethiopia',
  'Fiji',
  'Finland',
  'France',
  'Gabon',
  'Gambia',
  'Georgia',
  'Germany',
  'Ghana',
  'Greece',
  'Grenada',
  'Guatemala',
  'Guinea',
  'Guinea-Bissau',
  'Guyana',
  'Haiti',
  'Honduras',
  'Hungary',
  'Iceland',
  'India',
  'Indonesia',
  'Iran',
  'Iraq',
  'Ireland',
  'Israel',
  'Italy',
  'Ivory Coast',
  'Jamaica',
  'Japan',
  'Jordan',
  'Kazakhstan',
  'Kenya',
  'Kiribati',
  'Kuwait',
  'Kyrgyzstan',
  'Laos',
  'Latvia',
  'Lebanon',
  'Lesotho',
  'Liberia',
  'Libya',
  'Liechtenstein',
  'Lithuania',
  'Luxembourg',
  'Madagascar',
  'Malawi',
  'Malaysia',
  'Maldives',
  'Mali',
  'Malta',
  'Marshall Islands',
  'Mauritania',
  'Mauritius',
  'Mexico',
  'Micronesia',
  'Moldova',
  'Monaco',
  'Mongolia',
  'Montenegro',
  'Morocco',
  'Mozambique',
  'Myanmar',
  'Namibia',
  'Nauru',
  'Nepal',
  'Netherlands',
  'New Zealand',
  'Nicaragua',
  'Niger',
  'Nigeria',
  'North Korea',
  'North Macedonia',
  'Norway',
  'Oman',
  'Pakistan',
  'Palau',
  'Palestine',
  'Panama',
  'Papua New Guinea',
  'Paraguay',
  'Peru',
  'Philippines',
  'Poland',
  'Portugal',
  'Qatar',
  'Romania',
  'Russia',
  'Rwanda',
  'Saint Kitts and Nevis',
  'Saint Lucia',
  'Saint Vincent and the Grenadines',
  'Samoa',
  'San Marino',
  'Sao Tome and Principe',
  'Saudi Arabia',
  'Senegal',
  'Serbia',
  'Seychelles',
  'Sierra Leone',
  'Singapore',
  'Slovakia',
  'Slovenia',
  'Solomon Islands',
  'Somalia',
  'South Africa',
  'South Korea',
  'South Sudan',
  'Spain',
  'Sri Lanka',
  'Sudan',
  'Suriname',
  'Sweden',
  'Switzerland',
  'Syria',
  'Taiwan',
  'Tajikistan',
  'Tanzania',
  'Thailand',
  'Timor-Leste',
  'Togo',
  'Tonga',
  'Trinidad and Tobago',
  'Tunisia',
  'Turkey',
  'Turkmenistan',
  'Tuvalu',
  'Uganda',
  'Ukraine',
  'United Arab Emirates',
  'United Kingdom',
  'United States',
  'Uruguay',
  'Uzbekistan',
  'Vanuatu',
  'Vatican City',
  'Venezuela',
  'Vietnam',
  'Yemen',
  'Zambia',
  'Zimbabwe',
]

const handleSubmit = async () => {
  try {
    if (!userForm.value.username || !userForm.value.country) {
      error.value = 'Please fill in all required fields'
      return
    }

    const response = await apiClient.post('/users/create', userForm.value)

    if (response.status === 201) {
      Cookies.set('user', JSON.stringify(response.data), { expires: 7 })
      router.push('/')
    } else {
      console.error('Unexpected response status:', response.status)
      error.value = 'Error creating user profile'
    }
  } catch (err) {
    error.value = 'Error creating user profile'
    console.error(err)
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-100 py-6 flex flex-col justify-center sm:py-12">
    <div class="relative py-3 sm:max-w-xl sm:mx-auto">
      <div class="relative px-4 py-10 bg-white mx-8 md:mx-0 shadow rounded-3xl sm:p-10">
        <div class="max-w-md mx-auto">
          <div class="divide-y divide-gray-200">
            <div class="py-8 text-base leading-6 space-y-4 text-gray-700 sm:text-lg sm:leading-7">
              <h2 class="text-2xl font-bold mb-8 text-center">Complete Your Profile</h2>

              <div v-if="isLoading" class="text-center">Loading...</div>

              <div v-else-if="error" class="text-red-600 text-center mb-4">
                {{ error }}
              </div>

              <div v-else class="space-y-4">
                <!-- Profile Picture -->
                <div class="flex justify-center mb-6">
                  <img
                    :src="userForm.picture"
                    alt="Profile"
                    class="w-24 h-24 rounded-full border-2 border-gray-200"
                  />
                </div>

                <!-- Pre-filled Fields -->
                <div class="space-y-2">
                  <label class="text-gray-600">Name</label>
                  <input
                    v-model="userForm.name"
                    type="text"
                    disabled
                    class="w-full px-4 py-2 border rounded-md bg-gray-50"
                  />
                </div>

                <div class="space-y-2">
                  <label class="text-gray-600">Email</label>
                  <input
                    v-model="userForm.email"
                    type="email"
                    disabled
                    class="w-full px-4 py-2 border rounded-md bg-gray-50"
                  />
                </div>

                <!-- Required Fields -->
                <div class="space-y-2">
                  <label class="text-gray-600">Username*</label>
                  <input
                    v-model="userForm.username"
                    type="text"
                    class="w-full px-4 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Choose a username"
                    required
                  />
                </div>

                <div class="space-y-2">
                  <label class="text-gray-600">Country of Residence*</label>
                  <select
                    v-model="userForm.country"
                    class="w-full px-4 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Select a country</option>
                    <option v-for="country in countries" :key="country" :value="country">
                      {{ country }}
                    </option>
                  </select>
                </div>

                <button
                  @click="handleSubmit"
                  class="w-full py-3 mt-6 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                >
                  Complete Registration
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
